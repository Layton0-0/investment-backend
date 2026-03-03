package com.investment.factor.scheduler;

import com.investment.domain.entity.Strategy;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.StrategyRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.execution.PipelineExecutor;
import com.investment.strategy.domain.StrategyStatus;
import com.investment.factor.service.CapitalDrawdownConstraintService;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.MarketCrashGateService;
import com.investment.factor.service.RiskGateService;
import com.investment.governance.GovernanceHaltService;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyWeights;
import com.investment.setting.service.SystemSettingService;
import com.investment.strategy.engine.MacroIndicatorProvider;
import com.investment.strategy.service.StrategyWeightResolver;
import com.investment.factor.service.TradingWindowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PipelineExecutionScheduler")
class PipelineExecutionSchedulerTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;
    @Mock
    private StrategyRepository strategyRepository;
    @Mock
    private PipelineExecutor pipelineExecutor;
    @Mock
    private RiskGateService riskGateService;
    @Mock
    private DailyLossLimitService dailyLossLimitService;
    @Mock
    private MarketCrashGateService marketCrashGateService;
    @Mock
    private MacroIndicatorProvider macroIndicatorProvider;
    @Mock
    private GovernanceHaltService governanceHaltService;
    @Mock
    private StrategyWeightResolver strategyWeightResolver;
    @Mock
    private SystemSettingService systemSettingService;
    @Mock
    private TradingWindowService tradingWindowService;
    @Mock
    private CapitalDrawdownConstraintService capitalDrawdownConstraintService;

    @InjectMocks
    private PipelineExecutionScheduler pipelineExecutionScheduler;

    @BeforeEach
    void setUpTradingWindow() {
        lenient().when(tradingWindowService.isInKrWindow()).thenReturn(true);
        lenient().when(tradingWindowService.isInUsWindow()).thenReturn(true);
        lenient().when(capitalDrawdownConstraintService.getCapitalMultiplier(anyString(), anyString())).thenReturn(BigDecimal.ONE);
    }

    @Test
    @DisplayName("자동투자 ON 계좌 없으면 파이프라인 미실행")
    void runScheduledPipeline_noSettings_skips() {
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of());

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, never()).run(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("시장 급락 게이트 신규 매수 불가 시 해당 계좌 스킵")
    void runScheduledPipeline_marketCrashGateDisallow_skipsAccount() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(BigDecimal.valueOf(10000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .pipelineAutoExecute(true)
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any())).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(marketCrashGateService.isNewBuyAllowed()).thenReturn(false);
        when(systemSettingService.getBoolean("pipeline.autoExecute")).thenReturn(false);
        when(systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital")).thenReturn(BigDecimal.ZERO);

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, never()).run(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("리스크 게이트 신규 매수 불가 시 해당 계좌 스킵")
    void runScheduledPipeline_riskGateDisallow_skipsAccount() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(BigDecimal.valueOf(10000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .pipelineAutoExecute(true)
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any()))
                .thenReturn(RiskGateService.RiskGateResult.disallow());
        when(systemSettingService.getBoolean("pipeline.autoExecute")).thenReturn(false);
        when(systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital")).thenReturn(BigDecimal.ZERO);

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, never()).run(any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("자동투자 ON 계좌 있으면 리스크 통과 후 파이프라인 run 호출")
    void runScheduledPipeline_withSettings_runsPipeline() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(BigDecimal.valueOf(10000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .pipelineAutoExecute(true)
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any())).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(marketCrashGateService.isNewBuyAllowed()).thenReturn(true);
        when(strategyWeightResolver.resolve(any(), any())).thenReturn(StrategyWeights.builder()
                .shortPct(new BigDecimal("0.2"))
                .midPct(new BigDecimal("0.4"))
                .longPct(new BigDecimal("0.4"))
                .regime("NORMAL")
                .build());
        when(dailyLossLimitService.getCurrentPortfolioValue(anyString())).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.isNewBuyAllowed(anyString())).thenReturn(true);
        when(governanceHaltService.isHalted(anyString(), anyString())).thenReturn(false);
        when(strategyRepository.findByAccountNoAndMarketAndStrategyType(anyString(), anyString(), any(StrategyType.class)))
                .thenReturn(Optional.empty());
        when(systemSettingService.getBoolean("pipeline.autoExecute")).thenReturn(true);
        when(systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital")).thenReturn(BigDecimal.ZERO);

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, times(6)).run(eq(LocalDate.now().minusDays(1)), anyString(), eq("1234567890"),
                any(StrategyType.class), any(BigDecimal.class), eq(true));
    }

    @Test
    @DisplayName("governance halt인 (market, strategyType)은 해당 run 스킵")
    void runScheduledPipeline_governanceHalt_skipsThatRun() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(BigDecimal.valueOf(10000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .pipelineAutoExecute(true)
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any())).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(marketCrashGateService.isNewBuyAllowed()).thenReturn(true);
        when(dailyLossLimitService.getCurrentPortfolioValue(anyString())).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.isNewBuyAllowed(anyString())).thenReturn(true);
        when(governanceHaltService.isHalted(anyString(), anyString())).thenReturn(false);
        when(governanceHaltService.isHalted(eq("KR"), eq("SHORT_TERM"))).thenReturn(true);
        when(strategyRepository.findByAccountNoAndMarketAndStrategyType(anyString(), anyString(), any(StrategyType.class)))
                .thenReturn(Optional.empty());
        when(strategyWeightResolver.resolve(any(), any())).thenReturn(StrategyWeights.builder()
                .shortPct(new BigDecimal("0.2"))
                .midPct(new BigDecimal("0.4"))
                .longPct(new BigDecimal("0.4"))
                .regime("NORMAL")
                .build());
        when(systemSettingService.getBoolean("pipeline.autoExecute")).thenReturn(true);
        when(systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital")).thenReturn(BigDecimal.ZERO);

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, times(5)).run(any(), anyString(), anyString(), any(StrategyType.class), any(BigDecimal.class), anyBoolean());
        verify(governanceHaltService, atLeast(1)).isHalted("KR", "SHORT_TERM");
    }

    @Test
    @DisplayName("전략이 STOPPED인 (market, strategyType)은 해당 run 스킵")
    void runScheduledPipeline_strategyStopped_skipsThatRun() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(BigDecimal.valueOf(10000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .pipelineAutoExecute(true)
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any())).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(marketCrashGateService.isNewBuyAllowed()).thenReturn(true);
        when(strategyWeightResolver.resolve(any(), any())).thenReturn(StrategyWeights.builder()
                .shortPct(new BigDecimal("0.2"))
                .midPct(new BigDecimal("0.4"))
                .longPct(new BigDecimal("0.4"))
                .regime("NORMAL")
                .build());
        when(dailyLossLimitService.getCurrentPortfolioValue(anyString())).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.isNewBuyAllowed(anyString())).thenReturn(true);
        when(governanceHaltService.isHalted(anyString(), anyString())).thenReturn(false);
        Strategy stoppedStrategy = Strategy.builder()
                .accountNo("1234567890")
                .market("KR")
                .strategyType(StrategyType.SHORT_TERM)
                .status(StrategyStatus.STOPPED)
                .build();
        when(strategyRepository.findByAccountNoAndMarketAndStrategyType(eq("1234567890"), eq("KR"), eq(StrategyType.SHORT_TERM)))
                .thenReturn(Optional.of(stoppedStrategy));
        when(strategyRepository.findByAccountNoAndMarketAndStrategyType(eq("1234567890"), eq("KR"), eq(StrategyType.MEDIUM_TERM)))
                .thenReturn(Optional.empty());
        when(strategyRepository.findByAccountNoAndMarketAndStrategyType(eq("1234567890"), eq("KR"), eq(StrategyType.LONG_TERM)))
                .thenReturn(Optional.empty());
        when(strategyRepository.findByAccountNoAndMarketAndStrategyType(eq("1234567890"), eq("US"), any(StrategyType.class)))
                .thenReturn(Optional.empty());
        when(systemSettingService.getBoolean("pipeline.autoExecute")).thenReturn(true);
        when(systemSettingService.getBigDecimal("pipeline.scheduler.defaultCapital")).thenReturn(BigDecimal.ZERO);

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, times(5)).run(any(), anyString(), eq("1234567890"), any(StrategyType.class), any(BigDecimal.class), anyBoolean());
    }
}
