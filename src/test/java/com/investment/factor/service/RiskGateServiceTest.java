package com.investment.factor.service;

import com.investment.config.RiskProperties;
import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.risk.service.RegimeDetectionService;
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
    @Mock
    private RegimeDetectionService regimeDetectionService;

    @InjectMocks
    private RiskGateService riskGateService;

    @BeforeEach
    void setUp() {
        lenient().when(riskProperties.getVixThreshold()).thenReturn(new BigDecimal("30"));
        lenient().when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("50"));
        lenient().when(riskProperties.isRegimeDetectionEnabled()).thenReturn(false);
        lenient().when(riskProperties.getDrawdownRecoveryThresholdPct()).thenReturn(new BigDecimal("0.10"));
        lenient().when(riskProperties.getDrawdownRecoveryExitPct()).thenReturn(new BigDecimal("0.05"));
        lenient().when(riskProperties.getDrawdownRecoveryScale()).thenReturn(new BigDecimal("0.5"));
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

    @Test
    @DisplayName("레짐 탐지 활성·BEAR 시 비중 50% 축소")
    void evaluate_regimeBear_reduceMultiplier() {
        when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);
        when(riskProperties.isRegimeDetectionEnabled()).thenReturn(true);
        when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("50"));
        when(regimeDetectionService.getCurrentRegime(null))
                .thenReturn(new RegimeDetectionService.RegimeResult(
                        MacroDashboardResponse.MarketRegime.BEAR, 0.85, null, null, new BigDecimal("35")));

        RiskGateService.RiskGateResult result = riskGateService.evaluate(new BigDecimal("25"));

        assertThat(result.isAllowNewBuy()).isTrue();
        assertThat(result.getSizeMultiplier()).isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    @DisplayName("P6-1 isDrawdownRecoveryMode - MDD 10% 이상 시 회복 모드 ON")
    void isDrawdownRecoveryMode_mddAboveThreshold_true() {
        assertThat(riskGateService.isDrawdownRecoveryMode(new BigDecimal("0.10"))).isTrue();
        assertThat(riskGateService.isDrawdownRecoveryMode(new BigDecimal("0.15"))).isTrue();
    }

    @Test
    @DisplayName("P6-1 isDrawdownRecoveryMode - MDD 5% 이하 회복 시 정상 복구")
    void isDrawdownRecoveryMode_mddAtOrBelowExit_false() {
        assertThat(riskGateService.isDrawdownRecoveryMode(new BigDecimal("0.05"))).isFalse();
        assertThat(riskGateService.isDrawdownRecoveryMode(new BigDecimal("0.02"))).isFalse();
    }

    @Test
    @DisplayName("P6-1 isDrawdownRecoveryMode - MDD null 시 false")
    void isDrawdownRecoveryMode_mddNull_false() {
        assertThat(riskGateService.isDrawdownRecoveryMode(null)).isFalse();
    }

    @Test
    @DisplayName("P6-1 getDrawdownRecoveryScale - 기본 0.5")
    void getDrawdownRecoveryScale_returnsConfigured() {
        assertThat(riskGateService.getDrawdownRecoveryScale()).isEqualByComparingTo(new BigDecimal("0.5"));
    }
}
