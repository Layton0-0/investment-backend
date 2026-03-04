package com.investment.factor.service;

import com.investment.domain.entity.SymbolSector;
import com.investment.domain.repository.SymbolSectorRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationPenaltyService")
class CorrelationPenaltyServiceTest {

    private static final LocalDate BAS_DT = LocalDate.of(2026, 3, 4);
    private static final BigDecimal TOTAL = new BigDecimal("10000000");

    @Mock
    private com.investment.domain.repository.DailyStockRepository dailyStockRepository;
    @Mock
    private SymbolSectorRepository symbolSectorRepository;

    private CorrelationPenaltyService correlationPenaltyService;

    @BeforeEach
    void setUp() {
        correlationPenaltyService = new CorrelationPenaltyService(dailyStockRepository, symbolSectorRepository);
        ReflectionTestUtils.setField(correlationPenaltyService, "sectorConcentrationLimitPct", new BigDecimal("0.30"));
    }

    @Test
    @DisplayName("단일 섹터 30% 초과 시 해당 섹터 비중 비례 축소")
    void applySectorConcentrationLimit_singleSectorExceeds30Percent_scalesDown() {
        // A,B,C 모두 IT 섹터 → 합 50% > 30% → scale = 0.30/0.50 = 0.6 적용
        List<PositionRecommendationDto> input = List.of(
                rec("A", new BigDecimal("2000000")),  // 20%
                rec("B", new BigDecimal("2000000")),  // 20%
                rec("C", new BigDecimal("1000000"))   // 10%  → 합 50%
        );
        when(symbolSectorRepository.findByMarketAndSymbolIn(eq("KR"), anyList()))
                .thenReturn(List.of(
                        sector("A", "KR", "IT"),
                        sector("B", "KR", "IT"),
                        sector("C", "KR", "IT")
                ));

        List<PositionRecommendationDto> result = correlationPenaltyService.applySectorConcentrationLimit(
                input, TOTAL, "KR");

        assertThat(result).isNotNull().hasSize(3);
        // 각 권장액이 0.6배로 축소되어야 함. 20%*0.6=12%, 10%*0.6=6% → 합 30%
        BigDecimal sumAmt = result.stream()
                .map(PositionRecommendationDto::getRecommendedAmt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sectorWeight = sumAmt.divide(TOTAL, 6, java.math.RoundingMode.HALF_UP);
        assertThat(sectorWeight).isLessThanOrEqualTo(new BigDecimal("0.31")); // 30% 한도 근처
        assertThat(result.get(0).getMethod()).contains("SECTOR_CAP");
    }

    @Test
    @DisplayName("다양한 섹터 분포 시 30% 미만이면 제한 미적용")
    void applySectorConcentrationLimit_diverseSectorsUnderLimit_unchanged() {
        // IT 20%, FIN 15%, HC 10% → 각각 30% 미만
        List<PositionRecommendationDto> input = List.of(
                rec("A", new BigDecimal("2000000")),  // IT 20%
                rec("B", new BigDecimal("1500000")),  // FIN 15%
                rec("C", new BigDecimal("1000000"))   // HC 10%
        );
        when(symbolSectorRepository.findByMarketAndSymbolIn(eq("US"), anyList()))
                .thenReturn(List.of(
                        sector("A", "US", "IT"),
                        sector("B", "US", "FIN"),
                        sector("C", "US", "HC")
                ));

        List<PositionRecommendationDto> result = correlationPenaltyService.applySectorConcentrationLimit(
                input, TOTAL, "US");

        assertThat(result).isNotNull().hasSize(3);
        assertThat(result.get(0).getRecommendedAmt()).isEqualByComparingTo(new BigDecimal("2000000"));
        assertThat(result.get(1).getRecommendedAmt()).isEqualByComparingTo(new BigDecimal("1500000"));
        assertThat(result.get(2).getRecommendedAmt()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(result.get(0).getMethod()).doesNotContain("SECTOR_CAP");
    }

    @Test
    @DisplayName("recommendations 비어 있으면 그대로 반환")
    void applySectorConcentrationLimit_emptyList_returnsEmpty() {
        List<PositionRecommendationDto> result = correlationPenaltyService.applySectorConcentrationLimit(
                List.of(), TOTAL, "KR");
        assertThat(result).isEmpty();
    }

    private static PositionRecommendationDto rec(String symbol, BigDecimal amt) {
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
            return PositionRecommendationDto.builder()
                    .basDt(BAS_DT).symbol(symbol).market("KR")
                    .recommendedAmt(BigDecimal.ZERO).recommendedQty(0L).method("ATR")
                    .entryPrice(BigDecimal.TEN).stopLoss(BigDecimal.ONE)
                    .build();
        }
        long qty = amt.divide(BigDecimal.TEN, 0, java.math.RoundingMode.DOWN).longValue();
        return PositionRecommendationDto.builder()
                .basDt(BAS_DT).symbol(symbol).market("KR")
                .recommendedAmt(amt).recommendedQty(qty).method("ATR")
                .entryPrice(BigDecimal.TEN).stopLoss(BigDecimal.ONE)
                .build();
    }

    private static SymbolSector sector(String symbol, String market, String sectorCode) {
        return SymbolSector.builder()
                .symbol(symbol).market(market).sectorCode(sectorCode)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
