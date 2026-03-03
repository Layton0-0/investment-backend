package com.investment.backtest.robo;

import com.investment.backtest.robo.dto.RoboBacktestRequest;
import com.investment.backtest.robo.dto.RoboBacktestResult;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.RoboBacktestProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 로보 어드바이저 리밸런싱 스케줄러.
 * 설정된 cron에 실행. 자동투자 ON + 로보 어드바이저 ON 계좌만 대상.
 * 실행 전 백테스트(최근 N개월) 통과 시에만 RoboRebalanceExecutor 호출.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoboRebalanceScheduler {

    private final TradingSettingRepository tradingSettingRepository;
    private final RoboBacktestService roboBacktestService;
    private final RoboPreExecutionResultStore preExecutionResultStore;
    private final RoboRebalanceExecutor roboRebalanceExecutor;
    private final RoboBacktestProperties roboBacktestProperties;
    private final SystemSettingService systemSettingService;

    /**
     * 수동/배치 트리거용. 실제 ETF 주문 여부는 DB 시스템 설정(pipeline.autoExecute)에 따름.
     */
    public void runNow() {
        boolean autoExecute = systemSettingService.getBoolean("pipeline.autoExecute");
        List<TradingSetting> settings = tradingSettingRepository
                .findAllByAutoTradingEnabledTrueAndRoboAdvisorEnabledTrue();
        if (settings.isEmpty()) {
            log.info("로보 리밸런싱 스킵: 대상 계좌 없음");
            return;
        }

        int lookbackMonths = roboBacktestProperties.getPreExecutionLookbackMonths();
        BigDecimal maxMddPct = roboBacktestProperties.getPreExecutionMaxMddPct();
        BigDecimal minSharpe = roboBacktestProperties.getPreExecutionMinSharpe();

        LocalDate end = LocalDate.now().minusDays(1);
        LocalDate start = end.minusMonths(lookbackMonths);

        for (TradingSetting setting : settings) {
            String accountNo = setting.getAccountNo();
            BigDecimal capital = setting.getMaxInvestmentAmount() != null
                    && setting.getMaxInvestmentAmount().compareTo(BigDecimal.ZERO) > 0
                            ? setting.getMaxInvestmentAmount()
                            : BigDecimal.valueOf(10_000_000);

            try {
                RoboBacktestRequest request = RoboBacktestRequest.builder()
                        .startDate(start)
                        .endDate(end)
                        .initialCapital(capital)
                        .build();
                RoboBacktestResult result = roboBacktestService.run(request);

                boolean mddOk = result.getMddPct() == null || result.getMddPct().abs().compareTo(maxMddPct) <= 0;
                boolean sharpeOk = result.getSharpeRatio() == null || result.getSharpeRatio().compareTo(minSharpe) >= 0;
                boolean passed = mddOk && sharpeOk;

                preExecutionResultStore.store(accountNo, result, passed);

                if (!autoExecute) {
                    log.info("로보 실행 스킵(auto-execute=false): accountNo={}, passed={}, MDD={}, Sharpe={}",
                            LogMaskingUtil.maskAccountNo(accountNo), passed, result.getMddPct(),
                            result.getSharpeRatio());
                    continue;
                }
                if (passed) {
                    log.info("로보 실행 전 백테스트 통과: accountNo={}, MDD={}, Sharpe={}",
                            LogMaskingUtil.maskAccountNo(accountNo), result.getMddPct(), result.getSharpeRatio());
                    roboRebalanceExecutor.executeRebalance(accountNo, capital);
                } else {
                    log.warn("로보 실행 전 백테스트 미통과(리밸런싱 스킵): accountNo={}, MDD={}, Sharpe={}",
                            LogMaskingUtil.maskAccountNo(accountNo), result.getMddPct(), result.getSharpeRatio());
                }
            } catch (Exception e) {
                log.warn("로보 리밸런싱 실패: accountNo={}, error={}", LogMaskingUtil.maskAccountNo(accountNo), e.getMessage(),
                        e);
            }
        }
    }
}
