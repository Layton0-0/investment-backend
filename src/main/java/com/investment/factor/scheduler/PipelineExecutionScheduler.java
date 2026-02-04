package com.investment.factor.scheduler;

import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.execution.PipelineExecutor;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.RiskGateService;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.engine.MacroIndicatorProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 4단계 파이프라인 실행 스케줄러.
 * 장 시작 후 1회(기본 09:10 KST) 실행. 자동투자 ON인 계좌만 대상으로, 계좌별 총자산·비율(단기/중기/장기)로 배분 후
 * KR/US × SHORT/MEDIUM/LONG run 호출.
 * 실행 전 리스크 게이트(레짐·VIX)·일일 손실 한도 검사 적용 (전문 투자자 흐름 P0).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineExecutionScheduler {

    private static final BigDecimal DEFAULT_SHORT = new BigDecimal("0.2");
    private static final BigDecimal DEFAULT_MEDIUM = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_LONG = new BigDecimal("0.4");

    private final TradingSettingRepository tradingSettingRepository;
    private final PipelineExecutor pipelineExecutor;
    private final RiskGateService riskGateService;
    private final DailyLossLimitService dailyLossLimitService;
    private final MacroIndicatorProvider macroIndicatorProvider;

    @Value("${investment.pipeline.auto-execute:false}")
    private boolean autoExecute = false;

    @Value("${investment.pipeline.scheduler.default-capital:0}")
    private BigDecimal defaultCapital = BigDecimal.ZERO;

    /**
     * 수동/배치 트리거용. forceDryRun가 null이면 설정(auto-execute)에 따르고, non-null이면 해당 값으로 실행.
     */
    public void runNow(Boolean forceDryRun) {
        LocalDate basDt = LocalDate.now().minusDays(1);
        List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
        if (settings.isEmpty()) {
            log.debug("파이프라인 실행 스킵: 자동투자 ON 계좌 없음");
            return;
        }
        boolean dryRun = forceDryRun != null ? forceDryRun : !autoExecute;

        for (TradingSetting setting : settings) {
            String accountNo = setting.getAccountNo();
            BigDecimal capital = setting.getMaxInvestmentAmount();
            if (capital == null || capital.compareTo(BigDecimal.ZERO) <= 0) {
                capital = defaultCapital != null && defaultCapital.compareTo(BigDecimal.ZERO) > 0
                        ? defaultCapital
                        : BigDecimal.ZERO;
            }
            if (capital.compareTo(BigDecimal.ZERO) <= 0) {
                log.debug("파이프라인 실행 스킵: accountNo={}, 자본 미설정", accountNo);
                continue;
            }
            var indicatorsOpt = macroIndicatorProvider.getCurrentIndicators();
            RiskGateService.RiskGateResult riskResult = indicatorsOpt
                    .map(riskGateService::evaluateWithIndicators)
                    .orElseGet(() -> riskGateService.evaluate(null));
            BigDecimal sizeMultiplier = riskResult.getSizeMultiplier() != null
                    ? riskResult.getSizeMultiplier()
                    : BigDecimal.ONE;
            if (!riskResult.isAllowNewBuy()) {
                log.info("파이프라인 실행 스킵: accountNo={}, 리스크 게이트 신규 매수 불가", accountNo);
                continue;
            }
            BigDecimal shortPct = setting.getShortTermRatio() != null ? setting.getShortTermRatio() : DEFAULT_SHORT;
            BigDecimal midPct = setting.getMediumTermRatio() != null ? setting.getMediumTermRatio() : DEFAULT_MEDIUM;
            BigDecimal longPct = setting.getLongTermRatio() != null ? setting.getLongTermRatio() : DEFAULT_LONG;
            BigDecimal shortCapital = capital.multiply(shortPct).multiply(sizeMultiplier).setScale(0,
                    RoundingMode.DOWN);
            BigDecimal midCapital = capital.multiply(midPct).multiply(sizeMultiplier).setScale(0, RoundingMode.DOWN);
            BigDecimal longCapital = capital.multiply(longPct).multiply(sizeMultiplier).setScale(0, RoundingMode.DOWN);
            BigDecimal currentValue = dailyLossLimitService.getCurrentPortfolioValue(accountNo);
            if (currentValue != null && currentValue.compareTo(BigDecimal.ZERO) > 0) {
                dailyLossLimitService.recordOpeningBalanceIfAbsent(accountNo, currentValue);
            }
            if (!dailyLossLimitService.isNewBuyAllowed(accountNo)) {
                log.info("파이프라인 실행 스킵: accountNo={}, 일일 손실 한도 초과", accountNo);
                continue;
            }
            try {
                runPipelineForAccount(basDt, accountNo, shortCapital, midCapital, longCapital, dryRun);
            } catch (Exception e) {
                log.warn("파이프라인 실행 실패: accountNo={}, error={}", accountNo, e.getMessage(), e);
            }
        }
    }

    private void runPipelineForAccount(LocalDate basDt, String accountNo,
            BigDecimal shortCapital, BigDecimal midCapital, BigDecimal longCapital, boolean dryRun) {
        pipelineExecutor.run(basDt, "KR", accountNo, StrategyType.SHORT_TERM, shortCapital, dryRun);
        pipelineExecutor.run(basDt, "KR", accountNo, StrategyType.MEDIUM_TERM, midCapital, dryRun);
        pipelineExecutor.run(basDt, "KR", accountNo, StrategyType.LONG_TERM, longCapital, dryRun);
        pipelineExecutor.run(basDt, "US", accountNo, StrategyType.SHORT_TERM, shortCapital, dryRun);
        pipelineExecutor.run(basDt, "US", accountNo, StrategyType.MEDIUM_TERM, midCapital, dryRun);
        pipelineExecutor.run(basDt, "US", accountNo, StrategyType.LONG_TERM, longCapital, dryRun);
        log.debug("파이프라인 실행 완료: accountNo={}, basDt={}, dryRun={}", accountNo, basDt, dryRun);
    }
}
