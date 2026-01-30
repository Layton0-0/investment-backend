package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactorCalculationService")
class FactorCalculationServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;
    @Mock
    private SignalScoreRepository signalScoreRepository;

    @Captor
    private ArgumentCaptor<List<SignalScore>> saveAllCaptor;

    @InjectMocks
    private FactorCalculationService factorCalculationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(factorCalculationService, "disparityMaDays", 20);
        ReflectionTestUtils.setField(factorCalculationService, "volatilityBreakoutK", new BigDecimal("0.5"));
        ReflectionTestUtils.setField(factorCalculationService, "liquidityMinTrdVal", 1_000_000_000L);
    }

    @Test
    @DisplayName("일별 데이터 없으면 0건 저장")
    void calculateAndSave_noData_returnsZero() {
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        int saved = factorCalculationService.calculateAndSave(LocalDate.of(2026, 1, 29), "KR");

        assertThat(saved).isZero();
        verify(signalScoreRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("일별 데이터·이력 있으면 이격도·변동성돌파·유동성 시그널 저장")
    void calculateAndSave_withHistory_savesSignals() {
        LocalDate basDt = LocalDate.of(2026, 1, 29);
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            LocalDate d = basDt.minusDays(24 - i);
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("005930")
                    .market("KR")
                    .openPrice(BigDecimal.valueOf(70000 + i * 100))
                    .highPrice(BigDecimal.valueOf(70500 + i * 100))
                    .lowPrice(BigDecimal.valueOf(69500 + i * 100))
                    .closePrice(BigDecimal.valueOf(70200 + i * 100))
                    .volume(1_000_000L)
                    .trdVal(70_000_000_000L)
                    .build());
        }
        when(dailyStockRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), eq(basDt)))
                .thenReturn(history);

        doNothing().when(signalScoreRepository).saveAll(saveAllCaptor.capture());

        int saved = factorCalculationService.calculateAndSave(basDt, "KR");

        assertThat(saved).isGreaterThan(0);
        List<SignalScore> savedList = saveAllCaptor.getValue();
        assertThat(savedList).isNotEmpty();
        assertThat(savedList.stream().map(SignalScore::getFactorType).distinct().toList())
                .containsAnyOf(FactorCalculationService.FACTOR_DISPARITY,
                        FactorCalculationService.FACTOR_VOLATILITY_BREAKOUT,
                        FactorCalculationService.FACTOR_LIQUIDITY);
    }
}
