package com.investment.factor.scheduler;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.factor.execution.PipelineExecutor;
import com.investment.factor.service.CapitalDrawdownConstraintService;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.DriftRebalancingService;
import com.investment.factor.service.MarketCrashGateService;
import com.investment.factor.service.RiskGateService;
import com.investment.domain.entity.Strategy;
import com.investment.governance.GovernanceHaltService;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyWeights;
import com.investment.strategy.engine.MacroIndicatorProvider;
import com.investment.setting.service.SystemSettingService;
import com.investment.strategy.service.StrategyWeightResolver;
import com.investment.factor.service.TradingWindowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private final StrategyRepository strategyRepository;
    private final PipelineExecutor pipelineExecutor;
    private final RiskGateService riskGateService;
    private final DailyLossLimitService dailyLossLimitService;
    private final MarketCrashGateService marketCrashGateService;
    private final MacroIndicatorProvider macroIndicatorProvider;
    private final GovernanceHaltService governanceHaltService;
    private final StrategyWeightResolver strategyWeightResolver;
    private final SystemSettingService systemSettingService;
    private final TradingWindowService tradingWindowService;
    private final CapitalDrawdownConstraintService capitalDrawdownConstraintService;
    private final DriftRebalancingService driftRebalancingService;

    /**
     * 수동/배치 트리거용. 실제 주문 여부는 DB 시스템 설정·계정별 pipelineAutoExecute에 따름.
     * marketFilter가 null이면 KR/US 모두 유리 시간대일 때만 실행; "KR"이면 KR만, "US"이면 US만(각각 해당 윈도우 내일 때만).
     */
    public void runNow() {
        runNow(null);
    }

    public void runNow(String marketFilter) {
        LocalDate basDt = LocalDate.now().minusDays(1);
        List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
        if (settings.isEmpty()) {
            log.info("파이프라인 실행 스킵: code={}, {}", PipelineSkipReason.NO_AUTO_TRADING_ACCOUNTS.getCode(),
                    PipelineSkipReason.NO_AUTO_TRADING_ACCOUNTS.getDescription());
            return;
        }
        boolean serverAutoExecute = systemSettingService.getBoolean("pipeline.autoExecute");
        BigDecimal defaultCapital = systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital");
        for (TradingSetting setting : settings) {
            String accountNo = setting.getAccountNo();
            boolean effectiveAutoExecute = setting.getPipelineAutoExecute() != null
                    ? setting.getPipelineAutoExecute()
                    : serverAutoExecute;
            if (!effectiveAutoExecute) {
                log.info("파이프라인 실행 스킵: code={}, accountNo={}, {}",
                        PipelineSkipReason.ACCOUNT_AUTO_EXECUTE_OFF.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                        PipelineSkipReason.ACCOUNT_AUTO_EXECUTE_OFF.getDescription());
                continue;
            }
            BigDecimal capital = setting.getMaxInvestmentAmount();
            if (capital == null || capital.compareTo(BigDecimal.ZERO) <= 0) {
                capital = defaultCapital != null && defaultCapital.compareTo(BigDecimal.ZERO) > 0
                        ? defaultCapital
                        : BigDecimal.ZERO;
            }
            if (capital.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("파이프라인 실행 스킵: code={}, accountNo={}, {}",
                        PipelineSkipReason.CAPITAL_NOT_SET.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                        PipelineSkipReason.CAPITAL_NOT_SET.getDescription());
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
                log.info("파이프라인 실행 스킵: code={}, accountNo={}, {}",
                        PipelineSkipReason.RISK_GATE_NO_BUY.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                        PipelineSkipReason.RISK_GATE_NO_BUY.getDescription());
                continue;
            }
            if (!marketCrashGateService.isNewBuyAllowed()) {
                log.info("파이프라인 실행 스킵: code={}, accountNo={}, {}",
                        PipelineSkipReason.MARKET_CRASH_GATE.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                        PipelineSkipReason.MARKET_CRASH_GATE.getDescription());
                continue;
            }
            StrategyWeights weights;
            try {
                weights = strategyWeightResolver.resolve(setting, indicatorsOpt);
            } catch (Exception e) {
                log.warn("전략 비중 결정 실패, 기본 비중 사용: accountNo={}, error={}", LogMaskingUtil.maskAccountNo(accountNo), e.getMessage());
                weights = StrategyWeights.builder()
                        .shortPct(DEFAULT_SHORT)
                        .midPct(DEFAULT_MEDIUM)
                        .longPct(DEFAULT_LONG)
                        .regime("FALLBACK")
                        .build();
            }
            BigDecimal shortPct = weights.getShortPct();
            BigDecimal midPct = weights.getMidPct();
            BigDecimal longPct = weights.getLongPct();
            BigDecimal shortCapital = capital.multiply(shortPct).multiply(sizeMultiplier).setScale(0,
                    RoundingMode.DOWN);
            BigDecimal midCapital = capital.multiply(midPct).multiply(sizeMultiplier).setScale(0, RoundingMode.DOWN);
            BigDecimal longCapital = capital.multiply(longPct).multiply(sizeMultiplier).setScale(0, RoundingMode.DOWN);
            BigDecimal currentValue = dailyLossLimitService.getCurrentPortfolioValue(accountNo);
            if (currentValue != null && currentValue.compareTo(BigDecimal.ZERO) > 0) {
                dailyLossLimitService.recordOpeningBalanceIfAbsent(accountNo, currentValue);
            }
            if (!dailyLossLimitService.isNewBuyAllowed(accountNo)) {
                log.info("파이프라인 실행 스킵: code={}, accountNo={}, {}",
                        PipelineSkipReason.DAILY_LOSS_LIMIT.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                        PipelineSkipReason.DAILY_LOSS_LIMIT.getDescription());
                continue;
            }
            try {
                runPipelineForAccount(basDt, accountNo, shortCapital, midCapital, longCapital, effectiveAutoExecute, marketFilter);
            } catch (Exception e) {
                log.warn("파이프라인 실행 스킵: code={}, accountNo={}, error={}",
                        PipelineSkipReason.RUN_FAILED.getCode(), LogMaskingUtil.maskAccountNo(accountNo), e.getMessage(), e);
            }
            if (marketFilter == null || "US".equals(marketFilter)) {
                try {
                    driftRebalancingService.checkAndExecuteDriftRebalance(setting.getUserId(), accountNo, "US");
                } catch (Exception e) {
                    log.warn("드리프트 리밸런스 검사/실행 스킵: accountNo={}", LogMaskingUtil.maskAccountNo(accountNo), e);
                }
            }
        }
    }

    private void runPipelineForAccount(LocalDate basDt, String accountNo,
            BigDecimal shortCapital, BigDecimal midCapital, BigDecimal longCapital, boolean autoExecute, String marketFilter) {
        boolean runKr = (marketFilter == null || "KR".equals(marketFilter)) && tradingWindowService.isInKrWindow();
        boolean runUs = (marketFilter == null || "US".equals(marketFilter)) && tradingWindowService.isInUsWindow();
        if ((marketFilter == null || "KR".equals(marketFilter)) && !runKr) {
            log.info("파이프라인 실행 스킵: code={}, market=KR, accountNo={}, {}",
                    PipelineSkipReason.OUTSIDE_TRADING_WINDOW.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                    PipelineSkipReason.OUTSIDE_TRADING_WINDOW.getDescription());
        }
        if (runKr) {
            runIfNotHalted(basDt, "KR", accountNo, StrategyType.SHORT_TERM, shortCapital, autoExecute);
            runIfNotHalted(basDt, "KR", accountNo, StrategyType.MEDIUM_TERM, midCapital, autoExecute);
            runIfNotHalted(basDt, "KR", accountNo, StrategyType.LONG_TERM, longCapital, autoExecute);
        }
        if ((marketFilter == null || "US".equals(marketFilter)) && !runUs) {
            log.info("파이프라인 실행 스킵: code={}, market=US, accountNo={}, {}",
                    PipelineSkipReason.OUTSIDE_TRADING_WINDOW.getCode(), LogMaskingUtil.maskAccountNo(accountNo),
                    PipelineSkipReason.OUTSIDE_TRADING_WINDOW.getDescription());
        }
        if (runUs) {
            runIfNotHalted(basDt, "US", accountNo, StrategyType.SHORT_TERM, shortCapital, autoExecute);
            runIfNotHalted(basDt, "US", accountNo, StrategyType.MEDIUM_TERM, midCapital, autoExecute);
            runIfNotHalted(basDt, "US", accountNo, StrategyType.LONG_TERM, longCapital, autoExecute);
        }
    }

    /** 한국장 오후 유리 구간(14:30~15:30). 14:35 KST에 KR만 실행. */
    @Scheduled(cron = "${investment.pipeline.execution-schedule-cron-kr-afternoon:0 35 14 * * MON-FRI}")
    public void runKrAfternoon() {
        runNow("KR");
    }

    /** 미국장 마감 직전 유리 구간(05:00~06:00 KST). 05:05 KST에 US만 실행. */
    @Scheduled(cron = "${investment.pipeline.execution-schedule-cron-us-close:0 5 5 * * MON-FRI}")
    public void runUsClose() {
        runNow("US");
    }

    private void runIfNotHalted(LocalDate basDt, String market, String accountNo,
            StrategyType strategyType, BigDecimal capital, boolean autoExecute) {
        if (governanceHaltService.isHalted(market, strategyType.name())) {
            log.info("파이프라인 실행 스킵: code={}, market={}, strategyType={}, accountNo={}, {}",
                    PipelineSkipReason.GOVERNANCE_HALT.getCode(), market, strategyType,
                    LogMaskingUtil.maskAccountNo(accountNo), PipelineSkipReason.GOVERNANCE_HALT.getDescription());
            return;
        }
        Optional<Strategy> strategyOpt = strategyRepository.findByAccountNoAndMarketAndStrategyType(accountNo, market, strategyType);
        if (strategyOpt.isPresent()) {
            Strategy strategy = strategyOpt.get();
            if (strategy.isStopped() || strategy.getStatus() == StrategyStatus.PAUSED) {
                log.info("파이프라인 실행 스킵: code={}, market={}, strategyType={}, accountNo={}, {}",
                        PipelineSkipReason.STRATEGY_STOPPED_OR_PAUSED.getCode(), market, strategyType,
                        LogMaskingUtil.maskAccountNo(accountNo),
                        PipelineSkipReason.STRATEGY_STOPPED_OR_PAUSED.getDescription());
                return;
            }
        }
        BigDecimal multiplier = capitalDrawdownConstraintService.getCapitalMultiplier(market, strategyType.name());
        BigDecimal effectiveCapital = capital.multiply(multiplier != null ? multiplier : BigDecimal.ONE).setScale(0, RoundingMode.DOWN);
        log.debug("파이프라인 실행: market={}, strategyType={}, accountNo={}", market, strategyType, LogMaskingUtil.maskAccountNo(accountNo));
        pipelineExecutor.run(basDt, market, accountNo, strategyType, effectiveCapital, autoExecute);
        log.debug("파이프라인 실행 완료: market={}, strategyType={}, accountNo={}", market, strategyType, LogMaskingUtil.maskAccountNo(accountNo));
    }
}
