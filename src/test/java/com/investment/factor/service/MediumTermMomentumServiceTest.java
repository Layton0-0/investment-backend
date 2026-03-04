package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediumTermMomentumService")
class MediumTermMomentumServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;

    @InjectMocks
    private MediumTermMomentumService mediumTermMomentumService;

    private static final String MARKET = "KR";
    private static final LocalDate AS_OF = LocalDate.of(2026, 2, 28);

    @Test
    @DisplayName("데이터 없으면 빈 순위·빈 상위 10% 반환")
    void computeMomentumRanking_emptyData_returnsEmpty() {
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq(MARKET), any(LocalDate.class), eq(AS_OF)))
                .thenReturn(List.of());

        var result = mediumTermMomentumService.computeMomentumRanking(MARKET, AS_OF);

        assertThat(result.getOrderedSymbols()).isEmpty();
        assertThat(result.getTop10PercentSymbols()).isEmpty();
    }

    @Test
    @DisplayName("1종목 데이터면 해당 종목만 상위 10%에 포함")
    void computeMomentumRanking_singleSymbol_includesInTop10() {
        List<DailyStock> series = List.of(
                dailyStock(AS_OF, "005930", "10000"),
                dailyStock(AS_OF.minusDays(21), "005930", "9000"),
                dailyStock(AS_OF.minusDays(63), "005930", "8000"),
                dailyStock(AS_OF.minusDays(126), "005930", "7000")
        );
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq(MARKET), any(LocalDate.class), eq(AS_OF)))
                .thenReturn(series);

        var result = mediumTermMomentumService.computeMomentumRanking(MARKET, AS_OF);

        assertThat(result.getOrderedSymbols()).containsExactly("005930");
        assertThat(result.getTop10PercentSymbols()).containsExactly("005930");
    }

    private static DailyStock dailyStock(LocalDate basDt, String symbol, String close) {
        return DailyStock.builder()
                .basDt(basDt)
                .symbol(symbol)
                .market(MARKET)
                .closePrice(new BigDecimal(close))
                .build();
    }
}
