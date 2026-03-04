package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.Universe;
import com.investment.factor.dto.BreakoutCandidateDto;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.UniverseRepository;
import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntradayBreakoutService")
class IntradayBreakoutServiceTest {

        @Mock
        private UniverseRepository universeRepository;
        @Mock
        private DailyStockRepository dailyStockRepository;
        @Mock
        private RealtimeMarketDataService realtimeMarketDataService;
        @Mock
        private FactorCalculationService factorCalculationService;

        @InjectMocks
        private IntradayBreakoutService intradayBreakoutService;

        private static final LocalDate TODAY = LocalDate.of(2026, 2, 3);
        private static final LocalDate YESTERDAY = TODAY.minusDays(1);
        private static final String MARKET = "KR";
        private static final BigDecimal CAPITAL = new BigDecimal("100000000");

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(intradayBreakoutService, "volatilityBreakoutK", new BigDecimal("0.5"));
                ReflectionTestUtils.setField(intradayBreakoutService, "breakoutPositionPct", new BigDecimal("0.005"));
        }

        @Test
        @DisplayName("capital null 또는 0 이하 시 빈 목록")
        void getBreakoutCandidates_noCapital_returnsEmpty() {
                assertThat(intradayBreakoutService.getBreakoutCandidates(TODAY, MARKET, null)).isEmpty();
                assertThat(intradayBreakoutService.getBreakoutCandidates(TODAY, MARKET, BigDecimal.ZERO)).isEmpty();
        }

        @Test
        @DisplayName("전일 유니버스 없음 시 빈 목록")
        void getBreakoutCandidates_noUniverse_returnsEmpty() {
                when(universeRepository.findByBasDtAndMarketOrderBySymbol(YESTERDAY, MARKET)).thenReturn(List.of());

                List<BreakoutCandidateDto> result = intradayBreakoutService.getBreakoutCandidates(TODAY, MARKET,
                                CAPITAL);

                assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("돌파 충족 종목만 후보 반환")
        void getBreakoutCandidates_breakoutMet_returnsCandidates() {
                when(factorCalculationService.getVolatilityBreakoutK(eq("005930"), eq(MARKET), eq(TODAY)))
                                .thenReturn(new BigDecimal("0.5"));
                when(universeRepository.findByBasDtAndMarketOrderBySymbol(YESTERDAY, MARKET))
                                .thenReturn(List.of(Universe.builder().basDt(YESTERDAY).market(MARKET).symbol("005930")
                                                .build()));
                DailyStock prev = DailyStock.builder()
                                .basDt(YESTERDAY).symbol("005930").market(MARKET)
                                .highPrice(new BigDecimal("72000")).lowPrice(new BigDecimal("70000"))
                                .closePrice(new BigDecimal("71000"))
                                .build();
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                                eq("005930"), eq(MARKET), eq(YESTERDAY), eq(YESTERDAY)))
                                .thenReturn(List.of(prev));
                // 시가 71000, Range 2000, k=0.5 → Target 72000. 현재가 72100 이면 돌파
                when(realtimeMarketDataService.getCurrentPrices(any()))
                                .thenReturn(Mono.just(List.of(
                                                CurrentPriceDto.builder()
                                                                .symbol("005930")
                                                                .currentPrice(new BigDecimal("72100"))
                                                                .openPrice(new BigDecimal("71000"))
                                                                .build())));

                List<BreakoutCandidateDto> result = intradayBreakoutService.getBreakoutCandidates(TODAY, MARKET,
                                CAPITAL);

                assertThat(result).hasSize(1);
                assertThat(result.get(0).getSymbol()).isEqualTo("005930");
                assertThat(result.get(0).getTargetPrice()).isEqualByComparingTo(new BigDecimal("72000"));
                assertThat(result.get(0).getCurrentPrice()).isEqualByComparingTo(new BigDecimal("72100"));
                assertThat(result.get(0).getRecommendedQty()).isGreaterThan(0);
        }

        @Test
        @DisplayName("현재가가 Target 미만 시 후보 제외")
        void getBreakoutCandidates_belowTarget_returnsEmpty() {
                when(factorCalculationService.getVolatilityBreakoutK(eq("005930"), eq(MARKET), eq(TODAY)))
                                .thenReturn(new BigDecimal("0.5"));
                when(universeRepository.findByBasDtAndMarketOrderBySymbol(YESTERDAY, MARKET))
                                .thenReturn(List.of(Universe.builder().basDt(YESTERDAY).market(MARKET).symbol("005930")
                                                .build()));
                DailyStock prev = DailyStock.builder()
                                .basDt(YESTERDAY).symbol("005930").market(MARKET)
                                .highPrice(new BigDecimal("72000")).lowPrice(new BigDecimal("70000"))
                                .closePrice(new BigDecimal("71000"))
                                .build();
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                                eq("005930"), eq(MARKET), eq(YESTERDAY), eq(YESTERDAY)))
                                .thenReturn(List.of(prev));
                when(realtimeMarketDataService.getCurrentPrices(any()))
                                .thenReturn(Mono.just(List.of(
                                                CurrentPriceDto.builder()
                                                                .symbol("005930")
                                                                .currentPrice(new BigDecimal("71500")) // Target 72000
                                                                                                       // 미만
                                                                .openPrice(new BigDecimal("71000"))
                                                                .build())));

                List<BreakoutCandidateDto> result = intradayBreakoutService.getBreakoutCandidates(TODAY, MARKET,
                                CAPITAL);

                assertThat(result).isEmpty();
        }
}
