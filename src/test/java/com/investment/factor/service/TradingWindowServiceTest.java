package com.investment.factor.service;

import com.investment.config.PipelineTradingWindowProperties;
import com.investment.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradingWindowService")
class TradingWindowServiceTest {

    @Mock
    private SystemSettingService systemSettingService;

    private PipelineTradingWindowProperties tradingWindowProperties;
    private TradingWindowService service;

    @BeforeEach
    void setUp() {
        tradingWindowProperties = new PipelineTradingWindowProperties();
        service = new TradingWindowService(tradingWindowProperties, systemSettingService);
        lenient().when(systemSettingService.getBoolean("pipeline.tradingWindowEnabled")).thenReturn(true);
    }

    @Test
    @DisplayName("isVolatilePeriod KR 9:00-9:10 구간이면 true")
    void isVolatilePeriod_krStart_true() {
        tradingWindowProperties.setAvoidVolatileWindow(true);
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 0))).isTrue();
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 5))).isTrue();
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 9))).isTrue();
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 10))).isFalse();
    }

    @Test
    @DisplayName("isVolatilePeriod KR 15:20-15:30 구간이면 true")
    void isVolatilePeriod_krEnd_true() {
        tradingWindowProperties.setAvoidVolatileWindow(true);
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 20))).isTrue();
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 25))).isTrue();
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 30))).isFalse();
    }

    @Test
    @DisplayName("isVolatilePeriod avoidVolatileWindow false면 항상 false")
    void isVolatilePeriod_disabled_alwaysFalse() {
        tradingWindowProperties.setAvoidVolatileWindow(false);
        assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 5))).isFalse();
    }

    @Test
    @DisplayName("isVolatilePeriod US 23:30-23:40 구간이면 true")
    void isVolatilePeriod_usStart_true() {
        tradingWindowProperties.setAvoidVolatileWindow(true);
        assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 30))).isTrue();
        assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 35))).isTrue();
        assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 40))).isFalse();
    }
}
