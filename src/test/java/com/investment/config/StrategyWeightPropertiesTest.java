package com.investment.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("StrategyWeightProperties 테스트")
class StrategyWeightPropertiesTest {

    @Test
    @DisplayName("기본값 minWeight maxWeight 및 enabled")
    void default_minMax_enabled() {
        StrategyWeightProperties props = new StrategyWeightProperties();
        assertEquals(0, new BigDecimal("0.05").compareTo(props.getMinWeight()));
        assertEquals(0, new BigDecimal("0.55").compareTo(props.getMaxWeight()));
        assertTrue(props.isEnabled());
    }

    @Test
    @DisplayName("기본 레짐별 비중 테이블")
    void default_regimeWeights() {
        StrategyWeightProperties props = new StrategyWeightProperties();
        assertNotNull(props.getRegimeWeights());
        StrategyWeightProperties.RegimeWeights highVol = props.getRegimeWeights().get("HIGH_VOLATILITY");
        assertNotNull(highVol);
        assertEquals(0, new BigDecimal("0.10").compareTo(highVol.getShortPct()));
        assertEquals(0, new BigDecimal("0.35").compareTo(highVol.getMidPct()));
        assertEquals(0, new BigDecimal("0.55").compareTo(highVol.getLongPct()));

        StrategyWeightProperties.RegimeWeights def = props.getRegimeWeights().get("default");
        assertNotNull(def);
        assertEquals(0, new BigDecimal("0.20").compareTo(def.getShortPct()));
        assertEquals(0, new BigDecimal("0.40").compareTo(def.getMidPct()));
        assertEquals(0, new BigDecimal("0.40").compareTo(def.getLongPct()));
    }
}
