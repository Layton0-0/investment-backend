package com.investment.factor.service;

import com.investment.config.FrictionCostProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FrictionCostService")
class FrictionCostServiceTest {

    private FrictionCostProperties frictionCostProperties;
    private FrictionCostService frictionCostService;

    @BeforeEach
    void setUp() {
        frictionCostProperties = new FrictionCostProperties();
        frictionCostService = new FrictionCostService(frictionCostProperties);
    }

    @Test
    @DisplayName("KR 왕복 비용률이 0보다 큼")
    void getRoundTripCostRate_kr_positive() {
        BigDecimal rate = frictionCostService.getRoundTripCostRate("KR");
        assertThat(rate).isNotNull().isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("US 왕복 비용률이 0보다 큼")
    void getRoundTripCostRate_us_positive() {
        BigDecimal rate = frictionCostService.getRoundTripCostRate("US");
        assertThat(rate).isNotNull().isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("notional 기준 왕복 비용 금액 KR")
    void getRoundTripCostAmount_kr() {
        BigDecimal notional = new BigDecimal("1000000");
        BigDecimal amount = frictionCostService.getRoundTripCostAmount("KR", notional, 10);
        assertThat(amount).isNotNull().isGreaterThanOrEqualTo(BigDecimal.ZERO);
        BigDecimal rate = frictionCostService.getRoundTripCostRate("KR");
        assertThat(amount).isEqualByComparingTo(notional.multiply(rate).setScale(0, java.math.RoundingMode.HALF_UP));
    }

    @Test
    @DisplayName("null market 시 비용률 0")
    void getRoundTripCostRate_nullMarket_returnsZero() {
        assertThat(frictionCostService.getRoundTripCostRate(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
