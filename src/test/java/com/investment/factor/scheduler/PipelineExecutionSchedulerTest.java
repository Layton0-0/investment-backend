package com.investment.factor.scheduler;

import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.execution.PipelineExecutor;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.RiskGateService;
import com.investment.governance.GovernanceHaltService;
import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.engine.MacroIndicatorProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private PipelineExecutor pipelineExecutor;
    @Mock
    private RiskGateService riskGateService;
    @Mock
    private DailyLossLimitService dailyLossLimitService;
    @Mock
    private MacroIndicatorProvider macroIndicatorProvider;
    @Mock
    private GovernanceHaltService governanceHaltService;

    @InjectMocks
    private PipelineExecutionScheduler pipelineExecutionScheduler;

    @Test
    @DisplayName("자동투자 ON 계좌 없으면 파이프라인 미실행")
    void runScheduledPipeline_noSettings_skips() {
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of());

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
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any()))
                .thenReturn(RiskGateService.RiskGateResult.disallow());
        ReflectionTestUtils.setField(pipelineExecutionScheduler, "autoExecute", false);

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
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any())).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(dailyLossLimitService.getCurrentPortfolioValue(anyString())).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.isNewBuyAllowed(anyString())).thenReturn(true);
        when(governanceHaltService.isHalted(anyString(), anyString())).thenReturn(false);
        ReflectionTestUtils.setField(pipelineExecutionScheduler, "autoExecute", false);

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
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));
        when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());
        when(riskGateService.evaluate(any())).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(dailyLossLimitService.getCurrentPortfolioValue(anyString())).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.isNewBuyAllowed(anyString())).thenReturn(true);
        when(governanceHaltService.isHalted(anyString(), anyString())).thenReturn(false);
        when(governanceHaltService.isHalted(eq("KR"), eq("SHORT_TERM"))).thenReturn(true);
        ReflectionTestUtils.setField(pipelineExecutionScheduler, "autoExecute", false);

        pipelineExecutionScheduler.runNow(null);

        verify(pipelineExecutor, times(5)).run(any(), anyString(), anyString(), any(StrategyType.class), any(BigDecimal.class), anyBoolean());
        verify(governanceHaltService, atLeast(1)).isHalted("KR", "SHORT_TERM");
    }
}
