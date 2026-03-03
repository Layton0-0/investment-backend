package com.investment.risk.service;

import com.investment.config.RiskProperties;
import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.risk.dto.MacroIndicatorDto;
import com.investment.setting.service.SystemSettingService;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import com.investment.strategy.engine.MacroIndicatorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MacroDashboardService 단위 테스트")
class MacroDashboardServiceTest {

    @Mock
    private MacroIndicatorProvider macroIndicatorProvider;

    @Mock
    private SystemSettingService systemSettingService;

    private RiskProperties riskProperties;
    private MacroDashboardService service;

    @BeforeEach
    void setUp() {
        riskProperties = new RiskProperties();
        riskProperties.setRegimeGateEnabled(true);
        riskProperties.setVixThreshold(new BigDecimal("30"));
        riskProperties.setReduceSizeOnHighVolPct(new BigDecimal("50"));

        service = new MacroDashboardServiceImpl(macroIndicatorProvider, riskProperties, systemSettingService);
    }

    @Nested
    @DisplayName("대시보드 조회 테스트")
    class DashboardTests {

        @Test
        @DisplayName("기본 대시보드 조회 - 모의 데이터 포함")
        void shouldReturnDashboardWithMockData() {
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());

            MacroDashboardResponse response = service.getDashboard();

            assertThat(response).isNotNull();
            assertThat(response.getRegime()).isNotNull();
            assertThat(response.getAllIndicators()).isNotEmpty();
            assertThat(response.getAllIndicators().size()).isGreaterThanOrEqualTo(10);
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("실제 VIX 데이터 포함 대시보드")
        void shouldReturnDashboardWithRealVix() {
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("25.5"))
                            .interestRate(new BigDecimal("5.25"))
                            .inflationRate(new BigDecimal("3.2"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();

            assertThat(response.getAllIndicators().get("VIX")).isNotNull();
            assertThat(response.getAllIndicators().get("VIX").getValue())
                    .isEqualByComparingTo(new BigDecimal("25.50"));
        }

        @Test
        @DisplayName("모든 카테고리 지표 포함")
        void shouldContainAllCategories() {
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());

            MacroDashboardResponse response = service.getDashboard();

            assertThat(response.getMarketIndicators()).isNotEmpty();
            assertThat(response.getInterestRateIndicators()).isNotEmpty();
            assertThat(response.getEconomyIndicators()).isNotEmpty();
            assertThat(response.getCurrencyIndicators()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("개별 지표 조회 테스트")
    class IndicatorTests {

        @Test
        @DisplayName("VIX 지표 조회")
        void shouldReturnVixIndicator() {
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());

            MacroIndicatorDto vix = service.getIndicator("VIX");

            assertThat(vix).isNotNull();
            assertThat(vix.getCode()).isEqualTo("VIX");
            assertThat(vix.getCategory()).isEqualTo(MacroIndicatorDto.IndicatorCategory.MARKET);
            assertThat(vix.getSignal()).isNotNull();
        }

        @Test
        @DisplayName("존재하지 않는 지표 조회")
        void shouldReturnNullForUnknownIndicator() {
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());

            MacroIndicatorDto indicator = service.getIndicator("UNKNOWN");

            assertThat(indicator).isNull();
        }

        @Test
        @DisplayName("대소문자 구분 없이 조회")
        void shouldBeCaseInsensitive() {
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());

            MacroIndicatorDto vix1 = service.getIndicator("vix");
            MacroIndicatorDto vix2 = service.getIndicator("VIX");

            assertThat(vix1).isNotNull();
            assertThat(vix2).isNotNull();
        }
    }

    @Nested
    @DisplayName("시장 상태(레짐) 테스트")
    class RegimeTests {

        @Test
        @DisplayName("시장 상태 조회")
        void shouldReturnMarketRegime() {
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.empty());

            MacroDashboardResponse.MarketRegime regime = service.getMarketRegime();

            assertThat(regime).isNotNull();
            assertThat(regime).isIn(
                    MacroDashboardResponse.MarketRegime.BULL,
                    MacroDashboardResponse.MarketRegime.BEAR,
                    MacroDashboardResponse.MarketRegime.NEUTRAL
            );
        }
    }

    @Nested
    @DisplayName("리스크 게이트 테스트")
    class RiskGateTests {

        @Test
        @DisplayName("VIX 높을 때 리스크 게이트 트리거")
        void shouldTriggerRiskGateWhenVixHigh() {
            when(systemSettingService.getBoolean("risk.regimeGateEnabled")).thenReturn(true);
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("35"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();

            assertThat(response.getRiskGateStatus().isTriggered()).isTrue();
        }

        @Test
        @DisplayName("VIX 낮을 때 리스크 게이트 미트리거")
        void shouldNotTriggerRiskGateWhenVixLow() {
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("15"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();

            assertThat(response.getRiskGateStatus().isTriggered()).isFalse();
        }

        @Test
        @DisplayName("리스크 게이트 비활성화 시 미트리거")
        void shouldNotTriggerWhenRiskGateDisabled() {
            riskProperties.setRegimeGateEnabled(false);
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("50"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();

            assertThat(response.getRiskGateStatus().isTriggered()).isFalse();
        }
    }

    @Nested
    @DisplayName("신호등(Signal) 테스트")
    class SignalTests {

        @Test
        @DisplayName("VIX 낮을 때 GREEN")
        void shouldBeGreenWhenVixLow() {
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("15"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();
            MacroIndicatorDto vix = response.getAllIndicators().get("VIX");

            assertThat(vix.getSignal()).isEqualTo(MacroIndicatorDto.SignalLevel.GREEN);
        }

        @Test
        @DisplayName("VIX 중간일 때 YELLOW")
        void shouldBeYellowWhenVixMedium() {
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("25"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();
            MacroIndicatorDto vix = response.getAllIndicators().get("VIX");

            assertThat(vix.getSignal()).isEqualTo(MacroIndicatorDto.SignalLevel.YELLOW);
        }

        @Test
        @DisplayName("VIX 높을 때 RED")
        void shouldBeRedWhenVixHigh() {
            MacroEconomicStrategyEngine.MacroEconomicIndicators indicators =
                    MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                            .vix(new BigDecimal("35"))
                            .build();
            when(macroIndicatorProvider.getCurrentIndicators()).thenReturn(Optional.of(indicators));

            MacroDashboardResponse response = service.getDashboard();
            MacroIndicatorDto vix = response.getAllIndicators().get("VIX");

            assertThat(vix.getSignal()).isEqualTo(MacroIndicatorDto.SignalLevel.RED);
        }
    }

    @Nested
    @DisplayName("지원 지표 목록 테스트")
    class SupportedIndicatorsTests {

        @Test
        @DisplayName("지원 지표 목록 조회")
        void shouldReturnSupportedIndicators() {
            var codes = service.getSupportedIndicatorCodes();

            assertThat(codes).isNotEmpty();
            assertThat(codes).contains("VIX", "US10Y", "CPI", "DXY");
            assertThat(codes.size()).isGreaterThanOrEqualTo(10);
        }
    }
}
