package com.investment.factor.service;

import com.investment.config.RiskProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MarketCrashGateService")
class MarketCrashGateServiceTest {

    @Mock
    private RiskProperties riskProperties;
    @Mock
    private DailyStockRepository dailyStockRepository;

    @InjectMocks
    private MarketCrashGateService marketCrashGateService;

    private static final LocalDate YESTERDAY = LocalDate.now().minusDays(1);
    private static final LocalDate DAY_BEFORE = YESTERDAY.minusDays(1);

    @Test
    @DisplayName("게이트 비활성화 시 항상 허용")
    void isNewBuyAllowed_gateDisabled_returnsTrue() {
        when(riskProperties.isMarketCrashGateEnabled()).thenReturn(false);

        boolean result = marketCrashGateService.isNewBuyAllowed();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("벤치마크 데이터 부족 시 허용 (fail-open)")
    void isNewBuyAllowed_noData_returnsTrue() {
        when(riskProperties.isMarketCrashGateEnabled()).thenReturn(true);
        when(riskProperties.getMarketCrashDailyDropPct()).thenReturn(new BigDecimal("5"));
        when(riskProperties.getMarketCrashBenchmarkSymbol()).thenReturn("SPY");
        when(riskProperties.getMarketCrashBenchmarkMarket()).thenReturn("US");
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), eq(DAY_BEFORE), eq(YESTERDAY)))
                .thenReturn(List.of());

        boolean result = marketCrashGateService.isNewBuyAllowed();

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("전일 수익률 -6% 시 신규 매수 불가")
    void isNewBuyAllowed_crashBelowThreshold_returnsFalse() {
        when(riskProperties.isMarketCrashGateEnabled()).thenReturn(true);
        when(riskProperties.getMarketCrashDailyDropPct()).thenReturn(new BigDecimal("5"));
        when(riskProperties.getMarketCrashBenchmarkSymbol()).thenReturn("SPY");
        when(riskProperties.getMarketCrashBenchmarkMarket()).thenReturn("US");
        DailyStock prev = DailyStock.builder()
                .basDt(DAY_BEFORE)
                .symbol("SPY")
                .market("US")
                .closePrice(new BigDecimal("100"))
                .build();
        DailyStock curr = DailyStock.builder()
                .basDt(YESTERDAY)
                .symbol("SPY")
                .market("US")
                .closePrice(new BigDecimal("94"))
                .build();
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), eq(DAY_BEFORE), eq(YESTERDAY)))
                .thenReturn(List.of(prev, curr));

        boolean result = marketCrashGateService.isNewBuyAllowed();

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("전일 수익률 -4% 시 허용")
    void isNewBuyAllowed_noCrash_returnsTrue() {
        when(riskProperties.isMarketCrashGateEnabled()).thenReturn(true);
        when(riskProperties.getMarketCrashDailyDropPct()).thenReturn(new BigDecimal("5"));
        when(riskProperties.getMarketCrashBenchmarkSymbol()).thenReturn("SPY");
        when(riskProperties.getMarketCrashBenchmarkMarket()).thenReturn("US");
        DailyStock prev = DailyStock.builder()
                .basDt(DAY_BEFORE)
                .symbol("SPY")
                .market("US")
                .closePrice(new BigDecimal("100"))
                .build();
        DailyStock curr = DailyStock.builder()
                .basDt(YESTERDAY)
                .symbol("SPY")
                .market("US")
                .closePrice(new BigDecimal("96"))
                .build();
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("SPY"), eq("US"), eq(DAY_BEFORE), eq(YESTERDAY)))
                .thenReturn(List.of(prev, curr));

        boolean result = marketCrashGateService.isNewBuyAllowed();

        assertThat(result).isTrue();
    }
}
