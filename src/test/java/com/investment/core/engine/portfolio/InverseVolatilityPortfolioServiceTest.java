package com.investment.core.engine.portfolio;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InverseVolatilityPortfolioService")
class InverseVolatilityPortfolioServiceTest {

    private static final LocalDate BAS_DT = LocalDate.of(2026, 2, 1);
    private static final String MARKET = "KR";
    private static final BigDecimal TOTAL_CAPITAL = new BigDecimal("100000000");

    @Mock
    private DailyStockRepository dailyStockRepository;

    @InjectMocks
    private InverseVolatilityPortfolioService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "maxAllocationPct", new BigDecimal("0.2"));
    }

    @Test
    @DisplayName("변동성 높은 종목 비중이 변동성 낮은 종목 비중보다 작다")
    void applyInverseVolatilityWeights_lowVolGetsHigherWeightThanHighVol() {
        // 저변동성 종목 A: 수익률 변화 작음 → sigma 작음 → 역가중 시 비중 큼
        List<DailyStock> historyLowVol = dailyHistory(BAS_DT, 25, "005930", 100, 0.002);
        // 고변동성 종목 B: 수익률 변화 큼 → sigma 큼 → 역가중 시 비중 작음
        List<DailyStock> historyHighVol = dailyHistory(BAS_DT, 25, "000660", 100, 0.02);

        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("005930"), eq(MARKET), any(LocalDate.class), eq(BAS_DT)))
                .thenReturn(historyLowVol);
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("000660"), eq(MARKET), any(LocalDate.class), eq(BAS_DT)))
                .thenReturn(historyHighVol);

        List<PositionRecommendationDto> input = List.of(
                PositionRecommendationDto.builder()
                        .basDt(BAS_DT).symbol("005930").market(MARKET)
                        .recommendedAmt(new BigDecimal("5000000")).recommendedQty(50)
                        .method("ATR").entryPrice(new BigDecimal("100000")).stopLoss(new BigDecimal("98000"))
                        .build(),
                PositionRecommendationDto.builder()
                        .basDt(BAS_DT).symbol("000660").market(MARKET)
                        .recommendedAmt(new BigDecimal("5000000")).recommendedQty(50)
                        .method("ATR").entryPrice(new BigDecimal("100000")).stopLoss(new BigDecimal("98000"))
                        .build()
        );

        List<PositionRecommendationDto> result = service.applyInverseVolatilityWeights(
                input, TOTAL_CAPITAL, BAS_DT, MARKET);

        assertThat(result).hasSize(2);
        BigDecimal amtLowVol = result.stream().filter(d -> "005930".equals(d.getSymbol())).findFirst()
                .map(PositionRecommendationDto::getRecommendedAmt).orElse(BigDecimal.ZERO);
        BigDecimal amtHighVol = result.stream().filter(d -> "000660".equals(d.getSymbol())).findFirst()
                .map(PositionRecommendationDto::getRecommendedAmt).orElse(BigDecimal.ZERO);
        assertThat(amtLowVol).isGreaterThan(amtHighVol);
    }

    @Test
    @DisplayName("1종목 이하면 입력 그대로 반환")
    void applyInverseVolatilityWeights_emptyOrSingle_returnsAsIs() {
        List<PositionRecommendationDto> empty = List.of();
        assertThat(service.applyInverseVolatilityWeights(empty, TOTAL_CAPITAL, BAS_DT, MARKET)).isEmpty();

        List<PositionRecommendationDto> single = List.of(
                PositionRecommendationDto.builder()
                        .basDt(BAS_DT).symbol("005930").market(MARKET)
                        .recommendedAmt(new BigDecimal("1000000")).recommendedQty(10)
                        .entryPrice(new BigDecimal("100000")).stopLoss(new BigDecimal("99000")).build());
        assertThat(service.applyInverseVolatilityWeights(single, TOTAL_CAPITAL, BAS_DT, MARKET))
                .isEqualTo(single);
    }

    @Test
    @DisplayName("totalCapital 0 이하면 입력 그대로 반환")
    void applyInverseVolatilityWeights_zeroCapital_returnsAsIs() {
        List<PositionRecommendationDto> input = List.of(
                PositionRecommendationDto.builder()
                        .basDt(BAS_DT).symbol("005930").market(MARKET)
                        .recommendedAmt(new BigDecimal("1000000")).recommendedQty(10)
                        .entryPrice(new BigDecimal("100000")).stopLoss(new BigDecimal("99000")).build(),
                PositionRecommendationDto.builder()
                        .basDt(BAS_DT).symbol("000660").market(MARKET)
                        .recommendedAmt(new BigDecimal("1000000")).recommendedQty(10)
                        .entryPrice(new BigDecimal("100000")).stopLoss(new BigDecimal("99000")).build());
        List<PositionRecommendationDto> result = service.applyInverseVolatilityWeights(
                input, BigDecimal.ZERO, BAS_DT, MARKET);
        assertThat(result).isEqualTo(input);
    }

    private static List<DailyStock> dailyHistory(LocalDate endDt, int days, String symbol, double basePrice, double dailyReturnVol) {
        List<DailyStock> list = new ArrayList<>();
        double price = basePrice;
        LocalDateTime createdAt = LocalDateTime.now();
        for (int i = days; i >= 0; i--) {
            LocalDate dt = endDt.minusDays(i);
            list.add(DailyStock.builder()
                    .basDt(dt)
                    .symbol(symbol)
                    .market(MARKET)
                    .closePrice(BigDecimal.valueOf(price))
                    .openPrice(BigDecimal.valueOf(price))
                    .highPrice(BigDecimal.valueOf(price * 1.01))
                    .lowPrice(BigDecimal.valueOf(price * 0.99))
                    .createdAt(createdAt)
                    .build());
            price = price * (1 + dailyReturnVol * (i % 2 == 0 ? 1 : -1));
        }
        return list;
    }
}
