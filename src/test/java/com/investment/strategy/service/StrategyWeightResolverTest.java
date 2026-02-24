package com.investment.strategy.service;

import com.investment.config.StrategyWeightProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.strategy.dto.StrategyWeights;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StrategyWeightResolver 테스트")
class StrategyWeightResolverTest {

    @Mock
    private StrategyWeightProperties properties;

    @Mock
    private MacroEconomicStrategyEngine macroEngine;

    private StrategyWeightResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new StrategyWeightResolver(properties, macroEngine);
    }

    @Test
    @DisplayName("indicators 없으면 기본 비중")
    void no_indicators_returns_default() {
        when(properties.isEnabled()).thenReturn(true);
        TradingSetting setting = TradingSetting.builder().accountNo("x").build();
        StrategyWeights result = resolver.resolve(setting, Optional.empty());
        assertNotNull(result);
        assertEquals(0, new BigDecimal("0.2").compareTo(result.getShortPct()));
        assertEquals(0, new BigDecimal("0.4").compareTo(result.getMidPct()));
        assertEquals(0, new BigDecimal("0.4").compareTo(result.getLongPct()));
        assertEquals("NO_INDICATORS", result.getRegime());
        BigDecimal sum = result.getShortPct().add(result.getMidPct()).add(result.getLongPct());
        assertEquals(0, BigDecimal.ONE.compareTo(sum));
    }

    @Test
    @DisplayName("HIGH_VOLATILITY 레짐이면 목표 비중 근사")
    void high_volatility_regime_returns_tilted_weights() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getMinWeight()).thenReturn(new BigDecimal("0.05"));
        when(properties.getMaxWeight()).thenReturn(new BigDecimal("0.55"));
        Map<String, StrategyWeightProperties.RegimeWeights> map = new HashMap<>();
        map.put("HIGH_VOLATILITY", new StrategyWeightProperties.RegimeWeights("0.10", "0.35", "0.55"));
        when(properties.getRegimeWeights()).thenReturn(map);

        MacroEconomicStrategyEngine.MacroEconomicIndicators ind = MacroEconomicStrategyEngine.MacroEconomicIndicators.builder().build();
        MacroEconomicStrategyEngine.InvestmentStrategy strategy = MacroEconomicStrategyEngine.InvestmentStrategy.builder()
                .regime(MacroEconomicStrategyEngine.MarketRegime.HIGH_VOLATILITY)
                .build();
        when(macroEngine.decideStrategy(ind)).thenReturn(strategy);

        StrategyWeights result = resolver.resolve(TradingSetting.builder().accountNo("x").build(), Optional.of(ind));
        assertNotNull(result);
        assertTrue(result.getShortPct().compareTo(new BigDecimal("0.15")) < 0);
        assertTrue(result.getLongPct().compareTo(new BigDecimal("0.5")) > 0);
        assertEquals("HIGH_VOLATILITY", result.getRegime());
        BigDecimal sum = result.getShortPct().add(result.getMidPct()).add(result.getLongPct());
        assertEquals(0, BigDecimal.ONE.compareTo(sum));
    }

    @Test
    @DisplayName("비활성화 시 설정 비중 반환")
    void disabled_returns_setting_weights() {
        when(properties.isEnabled()).thenReturn(false);
        TradingSetting setting = TradingSetting.builder()
                .accountNo("test")
                .shortTermRatio(new BigDecimal("0.3"))
                .mediumTermRatio(new BigDecimal("0.3"))
                .longTermRatio(new BigDecimal("0.4"))
                .build();
        StrategyWeights result = resolver.resolve(setting, Optional.of(MacroEconomicStrategyEngine.MacroEconomicIndicators.builder().build()));
        assertNotNull(result);
        assertEquals(0, new BigDecimal("0.3").compareTo(result.getShortPct()));
        assertEquals(0, new BigDecimal("0.3").compareTo(result.getMidPct()));
        assertEquals(0, new BigDecimal("0.4").compareTo(result.getLongPct()));
        assertEquals("DISABLED", result.getRegime());
    }
}
