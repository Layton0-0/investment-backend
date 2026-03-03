package com.investment.factor.service;

import com.investment.config.RiskProperties;
import com.investment.setting.service.SystemSettingService;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskGateService")
class RiskGateServiceTest {

    @Mock
    private RiskProperties riskProperties;
    @Mock
    private MacroEconomicStrategyEngine macroEconomicStrategyEngine;
    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private RiskGateService riskGateService;

    @BeforeEach
    void setUp() {
        lenient().when(riskProperties.getVixThreshold()).thenReturn(new BigDecimal("30"));
        lenient().when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("50"));
    }

    @Test
    @DisplayName("레짐 게이트 비활성 시 항상 허용·배율 1.0")
    void evaluate_regimeGateDisabled_alwaysAllow() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(false);

        RiskGateService.RiskGateResult result = riskGateService.evaluate(new BigDecimal("40"));

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("VIX null 시 허용·배율 1.0")
    void evaluate_vixNull_allow() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);

        RiskGateService.RiskGateResult result = riskGateService.evaluate(null);

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("VIX 임계 이하 시 허용·배율 1.0")
    void evaluate_vixBelowThreshold_allow() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);

        RiskGateService.RiskGateResult result = riskGateService.evaluate(new BigDecimal("25"));

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    @DisplayName("VIX 임계 초과 시 허용·비중 축소 배율 적용")
    void evaluate_vixAboveThreshold_reduceMultiplier() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);
        when(riskProperties.getVixThreshold()).thenReturn(new BigDecimal("30"));
        when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("50"));

        RiskGateService.RiskGateResult result = riskGateService.evaluate(new BigDecimal("35"));

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("evaluateWithIndicators - 레짐 HIGH_VOLATILITY 시 비중 축소")
    void evaluateWithIndicators_highVolatility_reduceMultiplier() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);
        when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("50"));
        MacroEconomicStrategyEngine.MacroEconomicIndicators indicators = MacroEconomicStrategyEngine.MacroEconomicIndicators
                .builder().vix(new BigDecimal("32")).build();
        MacroEconomicStrategyEngine.InvestmentStrategy strategy = MacroEconomicStrategyEngine.InvestmentStrategy
                .builder()
                .regime(MacroEconomicStrategyEngine.MarketRegime.HIGH_VOLATILITY)
                .build();
        when(macroEconomicStrategyEngine.decideStrategy(any())).thenReturn(strategy);

        RiskGateService.RiskGateResult result = riskGateService.evaluateWithIndicators(indicators);

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("evaluateWithIndicators - indicators null 시 허용")
    void evaluateWithIndicators_null_allow() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);

        RiskGateService.RiskGateResult result = riskGateService.evaluateWithIndicators(null);

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(BigDecimal.ONE);
    }
}
