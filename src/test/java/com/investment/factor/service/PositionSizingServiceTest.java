package com.investment.factor.service;

import com.investment.core.engine.portfolio.InverseVolatilityPortfolioService;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.service.RiskReportService;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.news.service.NewsSignalService;
import com.investment.strategy.domain.StrategyType;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PositionSizingService")
class PositionSizingServiceTest {

        @Mock
        private SignalScoreRepository signalScoreRepository;
        @Mock
        private DailyStockRepository dailyStockRepository;
        @Mock
        private NewsSignalService newsSignalService;
        @Mock
        private FrictionCostService frictionCostService;
        @Mock
        private CorrelationPenaltyService correlationPenaltyService;
        @Mock
        private InverseVolatilityPortfolioService inverseVolatilityPortfolioService;
        @Mock
        private TradingSettingRepository tradingSettingRepository;
        @Mock
        private RiskReportService riskReportService;
        @Mock
        private RiskGateService riskGateService;

        @InjectMocks
        private PositionSizingService positionSizingService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(positionSizingService, "portfolioMode", "stub");
                ReflectionTestUtils.setField(positionSizingService, "positionRiskPct", new BigDecimal("0.01"));
                ReflectionTestUtils.setField(positionSizingService, "kellyP", new BigDecimal("0.6"));
                ReflectionTestUtils.setField(positionSizingService, "kellyB", new BigDecimal("2.0"));
                lenient().when(newsSignalService.getSymbolsWithSignalNews(any(), any())).thenReturn(java.util.Set.of());
                lenient().when(frictionCostService.getRoundTripCostRate(anyString())).thenReturn(new BigDecimal("0.002"));
                lenient().when(correlationPenaltyService.applyPenalty(anyList(), any(), anyString(), any()))
                        .thenAnswer(inv -> inv.getArgument(0));
                lenient().when(correlationPenaltyService.applySectorConcentrationLimit(anyList(), any(), anyString()))
                        .thenAnswer(inv -> inv.getArgument(0));
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

                when(signalScoreRepository.findByBasDtAndMarketOrderBySymbol(eq(basDt), eq(market),
                                any(Pageable.class)))
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
                // LONG_TERM 사용 시 심볼 필터(RSI/MACD/Smart Money) 없이 전체 시그널 기준으로 포지션 산출

                // when
                List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                                basDt, market, StrategyType.LONG_TERM, totalCapital);

                // then
                assertThat(recommendations).isNotEmpty();
                PositionRecommendationDto rec = recommendations.get(0);
                assertThat(rec.getSymbol()).isEqualTo("005930");
                assertThat(rec.getRecommendedQty()).isGreaterThan(0);
                assertThat(rec.getMethod()).isNotNull().matches(".*(ATR|KELLY).*");
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

        @Test
        @DisplayName("리스크 기반 캡 적용 시 종목당 권장 금액이 cap 초과하지 않음")
        void getRecommendations_withRiskBasedCap_capsPerSymbol() {
                LocalDate basDt = LocalDate.of(2026, 1, 30);
                String market = "KR";
                BigDecimal totalCapital = new BigDecimal("100000000"); // 1억, 5% = 500만
                ReflectionTestUtils.setField(positionSizingService, "riskBasedCapEnabled", true);
                ReflectionTestUtils.setField(positionSizingService, "riskBasedCapMaxPct", new BigDecimal("0.05"));

                SignalScore s1 = SignalScore.builder().basDt(basDt).symbol("005930").market(market).factorType("DISPARITY").score(BigDecimal.ONE).build();
                SignalScore s2 = SignalScore.builder().basDt(basDt).symbol("000660").market(market).factorType("DISPARITY").score(BigDecimal.ONE).build();
                when(signalScoreRepository.findByBasDtAndMarketOrderBySymbol(eq(basDt), eq(market), any(Pageable.class)))
                                .thenReturn(List.of(s1, s2));

                List<DailyStock> history1 = new ArrayList<>();
                List<DailyStock> history2 = new ArrayList<>();
                for (int i = 0; i < 40; i++) {
                        LocalDate d = basDt.minusDays(39 - i);
                        BigDecimal c = new BigDecimal("70000").add(new BigDecimal(i * 50));
                        history1.add(DailyStock.builder().basDt(d).symbol("005930").market(market).highPrice(c.add(BigDecimal.valueOf(500))).lowPrice(c.subtract(BigDecimal.valueOf(500))).closePrice(c).build());
                        history2.add(DailyStock.builder().basDt(d).symbol("000660").market(market).highPrice(c.add(BigDecimal.valueOf(500))).lowPrice(c.subtract(BigDecimal.valueOf(500))).closePrice(c).build());
                }
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(eq("005930"), eq(market), any(LocalDate.class), any(LocalDate.class))).thenReturn(history1);
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(eq("000660"), eq(market), any(LocalDate.class), any(LocalDate.class))).thenReturn(history2);

                List<PositionRecommendationDto> recommendations = positionSizingService.getRecommendations(
                                basDt, market, StrategyType.LONG_TERM, totalCapital);

                BigDecimal capAmt = totalCapital.multiply(new BigDecimal("0.05"));
                assertThat(recommendations).isNotEmpty();
                for (PositionRecommendationDto rec : recommendations) {
                        assertThat(rec.getRecommendedAmt()).isLessThanOrEqualTo(capAmt.add(BigDecimal.ONE));
                }
        }
}
