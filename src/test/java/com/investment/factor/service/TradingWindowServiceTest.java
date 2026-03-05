package com.investment.factor.service;

import com.investment.config.PipelineTradingWindowProperties;
import com.investment.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TradingWindowService")
class TradingWindowServiceTest {

    @Mock
    private SystemSettingService systemSettingService;

    private PipelineTradingWindowProperties properties;
    private TradingWindowService service;

    @BeforeEach
    void setUp() {
        properties = new PipelineTradingWindowProperties();
        properties.getKr().setStart("09:00");
        properties.getKr().setEnd("10:00");
        properties.getKr().setStart2("14:30");
        properties.getKr().setEnd2("15:30");
        properties.getUs().setStart("23:30");
        properties.getUs().setEnd("01:00");
        properties.getUs().setStart2("05:00");
        properties.getUs().setEnd2("06:00");
        lenient().when(systemSettingService.getBoolean("pipeline.tradingWindowEnabled")).thenReturn(true);
        service = new TradingWindowService(properties, systemSettingService, null);
    }

    @Nested
    @DisplayName("isInKrWindow(LocalTime)")
    class IsInKrWindow {
        @Test
        void firstSegment_returnsTrue() {
            assertThat(service.isInKrWindow(LocalTime.of(9, 0))).isTrue();
            assertThat(service.isInKrWindow(LocalTime.of(9, 30))).isTrue();
            assertThat(service.isInKrWindow(LocalTime.of(9, 59))).isTrue();
        }

        @Test
        void betweenSegments_returnsFalse() {
            assertThat(service.isInKrWindow(LocalTime.of(10, 0))).isFalse();
            assertThat(service.isInKrWindow(LocalTime.of(12, 0))).isFalse();
            assertThat(service.isInKrWindow(LocalTime.of(14, 29))).isFalse();
        }

        @Test
        void secondSegment_returnsTrue() {
            assertThat(service.isInKrWindow(LocalTime.of(14, 30))).isTrue();
            assertThat(service.isInKrWindow(LocalTime.of(15, 0))).isTrue();
            assertThat(service.isInKrWindow(LocalTime.of(15, 29))).isTrue();
        }

        @Test
        void afterSecondSegment_returnsFalse() {
            assertThat(service.isInKrWindow(LocalTime.of(15, 30))).isFalse();
            assertThat(service.isInKrWindow(LocalTime.of(16, 0))).isFalse();
        }
    }

    @Nested
    @DisplayName("isInUsWindow(LocalTime)")
    class IsInUsWindow {
        @Test
        void firstSegment_overMidnight_returnsTrue() {
            assertThat(service.isInUsWindow(LocalTime.of(23, 30))).isTrue();
            assertThat(service.isInUsWindow(LocalTime.of(0, 0))).isTrue();
            assertThat(service.isInUsWindow(LocalTime.of(0, 59))).isTrue();
        }

        @Test
        void betweenSegments_returnsFalse() {
            assertThat(service.isInUsWindow(LocalTime.of(1, 0))).isFalse();
            assertThat(service.isInUsWindow(LocalTime.of(3, 0))).isFalse();
            assertThat(service.isInUsWindow(LocalTime.of(4, 59))).isFalse();
        }

        @Test
        void secondSegment_returnsTrue() {
            assertThat(service.isInUsWindow(LocalTime.of(5, 0))).isTrue();
            assertThat(service.isInUsWindow(LocalTime.of(5, 30))).isTrue();
            assertThat(service.isInUsWindow(LocalTime.of(5, 59))).isTrue();
        }

        @Test
        void afterSecondSegment_returnsFalse() {
            assertThat(service.isInUsWindow(LocalTime.of(6, 0))).isFalse();
            assertThat(service.isInUsWindow(LocalTime.of(12, 0))).isFalse();
        }
    }

    @Nested
    @DisplayName("isVolatilePeriod")
    class IsVolatilePeriod {
        @Test
        void kr_volatileStart_returnsTrue() {
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 0))).isTrue();
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 5))).isTrue();
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 9))).isTrue();
        }

        @Test
        void kr_volatileEnd_returnsTrue() {
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 20))).isTrue();
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 25))).isTrue();
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 29))).isTrue();
        }

        @Test
        void kr_nonVolatile_returnsFalse() {
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 10))).isFalse();
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(10, 0))).isFalse();
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(15, 30))).isFalse();
        }

        @Test
        void us_volatileStart_returnsTrue() {
            assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 30))).isTrue();
            assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 35))).isTrue();
        }

        @Test
        void us_volatileEnd_returnsTrue() {
            assertThat(service.isVolatilePeriod("US", LocalTime.of(5, 50))).isTrue();
            assertThat(service.isVolatilePeriod("US", LocalTime.of(5, 59))).isTrue();
        }

        @Test
        void us_nonVolatile_returnsFalse() {
            assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 40))).isFalse();
            assertThat(service.isVolatilePeriod("US", LocalTime.of(1, 0))).isFalse();
        }

        @Test
        void avoidVolatileWindowFalse_returnsFalse() {
            properties.setAvoidVolatileWindow(false);
            service = new TradingWindowService(properties, systemSettingService, null);
            assertThat(service.isVolatilePeriod("KR", LocalTime.of(9, 5))).isFalse();
            assertThat(service.isVolatilePeriod("US", LocalTime.of(23, 35))).isFalse();
        }
    }

    @Test
    @DisplayName("tradingWindow disabled returns true for window checks")
    void windowDisabled_alwaysInWindow() {
        when(systemSettingService.getBoolean("pipeline.tradingWindowEnabled")).thenReturn(false);
        service = new TradingWindowService(properties, systemSettingService, null);
        assertThat(service.isInKrWindow(LocalTime.of(12, 0))).isTrue();
        assertThat(service.isInUsWindow(LocalTime.of(3, 0))).isTrue();
    }
}
