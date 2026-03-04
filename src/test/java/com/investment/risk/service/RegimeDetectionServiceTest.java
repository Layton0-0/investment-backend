package com.investment.risk.service;

import com.investment.config.RiskProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import com.investment.strategy.engine.MacroIndicatorProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegimeDetectionService")
class RegimeDetectionServiceTest {

    private static final LocalDate BASE = LocalDate.of(2026, 2, 1);

    @Mock
    private DailyStockRepository dailyStockRepository;
    @Mock
    private MacroIndicatorProvider macroIndicatorProvider;
    @Mock
    private RiskProperties riskProperties;

    @InjectMocks
    private RegimeDetectionServiceImpl regimeDetectionService;

    @BeforeEach
    void setUp() {
        when(riskProperties.isRegimeDetectionEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("레짐 탐지 비활성 시 NEUTRAL 반환")
    void getCurrentRegime_disabled_returnsNeutral() {
        when(riskProperties.isRegimeDetectionEnabled()).thenReturn(false);

        RegimeDetectionService.RegimeResult result = regimeDetectionService.getCurrentRegime(BASE);

        assertThat(result.regime()).isEqualTo(MacroDashboardResponse.MarketRegime.NEUTRAL);
        assertThat(result.confidence()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("SPY 200일 미만 시 VIX만으로 fallback - VIX 높으면 BEAR")
    void getCurrentRegime_insufficientSpy_highVix_returnsBear() {
        List<DailyStock> few = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            few.add(DailyStock.builder()
                    .basDt(BASE.minusDays(50 - i)).symbol("SPY").market("US")
                    .closePrice(BigDecimal.valueOf(500))
                    .build());
        }
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), any(LocalDate.class), eq(BASE)))
                .thenReturn(few);
        when(macroIndicatorProvider.getCurrentIndicators())
                .thenReturn(Optional.of(MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                        .vix(new BigDecimal("35"))
                        .build()));

        RegimeDetectionService.RegimeResult result = regimeDetectionService.getCurrentRegime(BASE);

        assertThat(result.regime()).isEqualTo(MacroDashboardResponse.MarketRegime.BEAR);
        assertThat(result.vix()).isEqualByComparingTo("35");
    }

    @Test
    @DisplayName("SPY 50>200 이평선 + VIX<20 이면 BULL")
    void getCurrentRegime_ma50AboveMa200_lowVix_returnsBull() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 230; i++) {
            LocalDate d = BASE.minusDays(229 - i);
            BigDecimal close = i >= 180 ? BigDecimal.valueOf(520) : BigDecimal.valueOf(480);
            history.add(DailyStock.builder()
                    .basDt(d).symbol("SPY").market("US")
                    .closePrice(close)
                    .build());
        }
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), any(LocalDate.class), eq(BASE)))
                .thenReturn(history);
        when(macroIndicatorProvider.getCurrentIndicators())
                .thenReturn(Optional.of(MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                        .vix(new BigDecimal("18"))
                        .build()));

        RegimeDetectionService.RegimeResult result = regimeDetectionService.getCurrentRegime(BASE);

        assertThat(result.regime()).isEqualTo(MacroDashboardResponse.MarketRegime.BULL);
        assertThat(result.spyMa50()).isNotNull();
        assertThat(result.spyMa200()).isNotNull();
        assertThat(result.spyMa50().compareTo(result.spyMa200())).isGreaterThan(0);
        assertThat(result.vix()).isEqualByComparingTo("18");
    }

    @Test
    @DisplayName("SPY 50<200 이평선 + VIX>30 이면 BEAR")
    void getCurrentRegime_ma50BelowMa200_highVix_returnsBear() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 230; i++) {
            LocalDate d = BASE.minusDays(229 - i);
            BigDecimal close = i >= 180 ? BigDecimal.valueOf(460) : BigDecimal.valueOf(500);
            history.add(DailyStock.builder()
                    .basDt(d).symbol("SPY").market("US")
                    .closePrice(close)
                    .build());
        }
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), any(LocalDate.class), eq(BASE)))
                .thenReturn(history);
        when(macroIndicatorProvider.getCurrentIndicators())
                .thenReturn(Optional.of(MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                        .vix(new BigDecimal("32"))
                        .build()));

        RegimeDetectionService.RegimeResult result = regimeDetectionService.getCurrentRegime(BASE);

        assertThat(result.regime()).isEqualTo(MacroDashboardResponse.MarketRegime.BEAR);
        assertThat(result.spyMa50().compareTo(result.spyMa200())).isLessThan(0);
    }

    @Test
    @DisplayName("그 외 조건이면 NEUTRAL")
    void getCurrentRegime_mixed_returnsNeutral() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 230; i++) {
            history.add(DailyStock.builder()
                    .basDt(BASE.minusDays(229 - i)).symbol("SPY").market("US")
                    .closePrice(BigDecimal.valueOf(500))
                    .build());
        }
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), any(LocalDate.class), eq(BASE)))
                .thenReturn(history);
        when(macroIndicatorProvider.getCurrentIndicators())
                .thenReturn(Optional.of(MacroEconomicStrategyEngine.MacroEconomicIndicators.builder()
                        .vix(new BigDecimal("25"))
                        .build()));

        RegimeDetectionService.RegimeResult result = regimeDetectionService.getCurrentRegime(BASE);

        assertThat(result.regime()).isEqualTo(MacroDashboardResponse.MarketRegime.NEUTRAL);
    }
}
