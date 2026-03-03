package com.investment.governance;

import com.investment.alert.EmergencyAlertService;
import com.investment.backtest.BacktestService;
import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.config.GovernanceProperties;
import com.investment.domain.entity.GovernanceCheckResult;
import com.investment.domain.repository.GovernanceCheckResultRepository;
import com.investment.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 전략 거버넌스 검사 — 정기 백테스트 실행 후 MDD/Sharpe 열화 시 Discord 알림.
 * 1차: 알림만 발송. 2차: 검사 결과 저장, 열화 시 (설정에 따라) halt 등록.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyGovernanceCheckService {

    private static final String COMPONENT_STRATEGY_GOVERNANCE = "StrategyGovernance";

    private final BacktestService backtestService;
    private final EmergencyAlertService emergencyAlertService;
    private final GovernanceProperties governanceProperties;
    private final GovernanceCheckResultRepository governanceCheckResultRepository;
    private final GovernanceHaltService governanceHaltService;
    private final SystemSettingService systemSettingService;

    private boolean isGovernanceEnabled() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("governance.enabled"));
    }

    private boolean isGovernanceAlertOnly() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("governance.alertOnly"));
    }

    private boolean isGovernanceAutoHaltOnDegradation() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("governance.autoHaltOnDegradation"));
    }

    /**
     * 최근 lookbackMonths 구간에 대해 백테스트를 실행하고, 결과 저장·열화 시 알림·(설정 시) halt 등록.
     */
    @Transactional
    public void checkAndSendAlerts() {
        if (!isGovernanceEnabled()) {
            log.debug("Governance disabled, skip check");
            return;
        }
        Instant runAt = Instant.now();
        int lookback = governanceProperties.getLookbackMonths();
        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusMonths(lookback);
        BigDecimal capital = governanceProperties.getDefaultCapital() != null
                ? governanceProperties.getDefaultCapital()
                : new BigDecimal("100000000");

        List<String> degradedMessages = new ArrayList<>();

        for (String market : List.of("KR", "US")) {
            for (String strategyType : List.of("SHORT_TERM", "MEDIUM_TERM")) {
                try {
                    BacktestRunRequest request = BacktestRunRequest.builder()
                            .startDate(start)
                            .endDate(end)
                            .market(market)
                            .strategyType(strategyType)
                            .initialCapital(capital)
                            .build();
                    BacktestRunResult result = backtestService.run(request);

                    boolean mddDegraded = isMddDegraded(result);
                    boolean sharpeDegraded = isSharpeDegraded(result);
                    boolean degraded = mddDegraded || sharpeDegraded;

                    governanceCheckResultRepository.save(GovernanceCheckResult.of(
                            runAt, market, strategyType,
                            result.getMddPct(), result.getSharpeRatio(), degraded,
                            start, end));

                    if (degraded) {
                        String msg = String.format(
                                "**[전략 거버넌스] 열화 감지**\n시장: %s, 전략: %s\n구간: %s ~ %s\nMDD: %s%% (기준: %s%%)\nSharpe: %s (기준 최소: %s)\n권장: 자동 매매 중단 후 원인 분석·재검증.",
                                market, strategyType,
                                start, end,
                                result.getMddPct() != null ? result.getMddPct().toPlainString() : "N/A",
                                governanceProperties.getMddThresholdPct().toPlainString(),
                                result.getSharpeRatio() != null ? result.getSharpeRatio().toPlainString() : "N/A",
                                governanceProperties.getSharpeMin().toPlainString());
                        degradedMessages.add(msg);

                        if (!isGovernanceAlertOnly() && isGovernanceAutoHaltOnDegradation()) {
                            governanceHaltService.setHalt(market, strategyType, msg);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Governance backtest failed: market={}, strategyType={}, error={}",
                            market, strategyType, e.getMessage());
                }
            }
        }

        if (!degradedMessages.isEmpty()) {
            String fullMessage = String.join("\n\n", degradedMessages);
            emergencyAlertService.sendRiskEventAlert("WARNING", COMPONENT_STRATEGY_GOVERNANCE, fullMessage);
            log.info("Strategy governance alert sent: {} degraded scenario(s)", degradedMessages.size());
        }
    }

    private boolean isMddDegraded(BacktestRunResult result) {
        BigDecimal mdd = result.getMddPct();
        BigDecimal threshold = governanceProperties.getMddThresholdPct();
        if (mdd == null || threshold == null) {
            return false;
        }
        return mdd.compareTo(threshold) < 0;
    }

    private boolean isSharpeDegraded(BacktestRunResult result) {
        BigDecimal sharpe = result.getSharpeRatio();
        BigDecimal min = governanceProperties.getSharpeMin();
        if (sharpe == null || min == null) {
            return false;
        }
        return sharpe.compareTo(min) < 0;
    }
}
