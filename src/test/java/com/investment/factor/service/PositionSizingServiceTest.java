package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositionSizingService")
class PositionSizingServiceTest {

    @Mock
    private SignalScoreRepository signalScoreRepository;
    @Mock
    private DailyStockRepository dailyStockRepository;

    @InjectMocks
    private PositionSizingService positionSizingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(positionSizingService, "positionRiskPct", new BigDecimal("0.01"));
        ReflectionTestUtils.setField(positionSizingService, "kellyP", new BigDecimal("0.6"));
        ReflectionTestUtils.setField(positionSizingService, "kellyB", new BigDecimal("2.0"));
    }

    @Test
    @DisplayName("Half-Kelly 적용 - 켈리 공식으로 포지션 사이징 조정")
    void getRecommendations_withHalfKelly_adjustsPositionSize() {
        // given
        LocalDate basDt = LocalDate.of(2026, 1, 30);
        String market = "KR";
        BigDecimal totalCapital = new BigDecimal("100000000"); // 1억원

        SignalScore signal = SignalScore.builder()
                .basDt(basDt)
                .symbol("005930")
                .market(market)
                .factorType("DISPARITY")
                .score(new BigDecimal("105.0"))
                .build();

        when(signalScoreRepository.findByBasDtAndMarketOrderBySymbol(eq(basDt), eq(market), any(Pageable.class)))
                .thenReturn(List.of(signal));

        // ATR·RSI·MACD 계산용 일별 데이터 (40일, 상승 추세 — SHORT_TERM RSI>60 & MACD>Signal 통과)
        List<DailyStock> history = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            BigDecimal close = new BigDecimal("70000").add(new BigDecimal(i * 50));
            LocalDate d = basDt.minusDays(39 - i);
            history.add(DailyStock.builder()
                    .basDt(d)
                    .symbol("005930")
                    .market(market)
                    .highPrice(close.add(new BigDecimal("500")))
                    .lowPrice(close.subtract(new BigDecimal("500")))
                    .closePrice(close)
                    .build());
        }
        when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                eq("005930"), eq(market), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(history);

        // when
        List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                basDt, market, totalCapital);

        // then
        assertThat(recommendations).isNotEmpty();
        PositionRecommendationDto rec = recommendations.get(0);
        assertThat(rec.getSymbol()).isEqualTo("005930");
        assertThat(rec.getRecommendedQty()).isGreaterThan(0);
        assertThat(rec.getMethod()).containsAnyOf("ATR", "KELLY");
    }

    @Test
    @DisplayName("총자산이 0이면 권장 목록 없음")
    void getRecommendations_zeroCapital_returnsEmpty() {
        // when
        List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                LocalDate.now(), "KR", BigDecimal.ZERO);

        // then
        assertThat(recommendations).isEmpty();
        verify(signalScoreRepository, never()).findByBasDtAndMarketOrderBySymbol(any(), any(), any());
    }

    @Test
    @DisplayName("시그널 없으면 권장 목록 없음")
    void getRecommendations_noSignals_returnsEmpty() {
        // given
        when(signalScoreRepository.findByBasDtAndMarketOrderBySymbol(any(), any(), any()))
                .thenReturn(List.of());

        // when
        List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                LocalDate.now(), "KR", new BigDecimal("100000000"));

        // then
        assertThat(recommendations).isEmpty();
    }
}
