package com.investment.factor.execution;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.StrategyPosition;
import com.investment.strategy.domain.StrategyType;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.StrategyPositionRepository;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExitRuleService")
class ExitRuleServiceTest {

        @Mock
        private StrategyPositionRepository strategyPositionRepository;
        @Mock
        private DailyStockRepository dailyStockRepository;
        @Mock
        private ExitRuleEvaluator exitRuleEvaluator;

        @InjectMocks
        private ExitRuleService exitRuleService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(exitRuleService, "atrTrailingStopMultiplier", new BigDecimal("2.0"));
        }

        @Test
        @DisplayName("ATR Trailing Stop 시그널 생성 - trailing_high 대비 현재가 하락이 ATR × multiplier 이상")
        void getSellSignals_atrTrailingStop_createsSignal() {
                // given
                LocalDate today = LocalDate.of(2026, 1, 30);
                String accountNo = "1234567890";
                String symbol = "005930";

                StrategyPosition position = StrategyPosition.builder()
                                .id(1L)
                                .accountNo(accountNo)
                                .symbol(symbol)
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .entryDt(today.minusDays(10))
                                .entryPrice(new BigDecimal("70000"))
                                .quantity(10)
                                .trailingHigh(new BigDecimal("80000")) // 최고가
                                .atrMultiplier(new BigDecimal("2.0"))
                                .timeCutDays(5)
                                .targetReturnPct(new BigDecimal("3.0"))
                                .build();

                when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo))
                                .thenReturn(List.of(position));

                // ATR 계산용 일별 데이터 (14일 + 여유)
                List<DailyStock> history = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                        LocalDate d = today.minusDays(19 - i);
                        history.add(DailyStock.builder()
                                        .basDt(d)
                                        .symbol(symbol)
                                        .market("KR")
                                        .highPrice(new BigDecimal("75000"))
                                        .lowPrice(new BigDecimal("70000"))
                                        .closePrice(new BigDecimal("72000"))
                                        .build());
                }
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                                eq(symbol), eq("KR"), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(history);

                when(exitRuleEvaluator.evaluate(any(ExitRuleInput.class)))
                                .thenReturn(ExitRuleResult.exit("SHORT_TERM_TRAILING_STOP"));

                BigDecimal currentPrice = new BigDecimal("69000");
                Map<String, BigDecimal> currentPriceBySymbol = new HashMap<>();
                currentPriceBySymbol.put(symbol, currentPrice);

                // when
                List<ExitRuleService.ExitSignal> signals = exitRuleService.getSellSignals(accountNo,
                                currentPriceBySymbol);

                // then — SHORT_TERM: -3% Trailing Stop (현재가 69000 ≤ 80000×0.97=77600)
                assertThat(signals).hasSize(1);
                assertThat(signals.get(0).getReason()).isEqualTo("SHORT_TERM_TRAILING_STOP");
                assertThat(signals.get(0).getSymbol()).isEqualTo(symbol);
                assertThat(signals.get(0).getCurrentPrice()).isEqualByComparingTo(currentPrice);
                verify(strategyPositionRepository).save(any(StrategyPosition.class)); // trailingHigh 갱신
        }

        @Test
        @DisplayName("Time-Cut 시그널 생성 - 목표 수익률 미도달 (SHORT_TERM 전용)")
        void getSellSignals_timeCut_createsSignal() {
                // given — Time-Cut은 SHORT_TERM에만 적용
                LocalDate today = LocalDate.of(2026, 1, 30);
                String accountNo = "1234567890";
                String symbol = "005930";

                StrategyPosition position = StrategyPosition.builder()
                                .id(1L)
                                .accountNo(accountNo)
                                .symbol(symbol)
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .entryDt(today.minusDays(6)) // 6일 경과 (timeCutDays=5 초과)
                                .entryPrice(new BigDecimal("70000"))
                                .quantity(10)
                                .trailingHigh(new BigDecimal("71000"))
                                .atrMultiplier(new BigDecimal("2.0"))
                                .timeCutDays(5)
                                .targetReturnPct(new BigDecimal("3.0")) // 목표 3%
                                .build();

                when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo))
                                .thenReturn(List.of(position));

                // ATR Trailing Stop 조건 미충족 (현재가가 trailing_high 근처)
                BigDecimal currentPrice = new BigDecimal("70500"); // 수익률 약 0.7% (목표 3% 미달)
                Map<String, BigDecimal> currentPriceBySymbol = new HashMap<>();
                currentPriceBySymbol.put(symbol, currentPrice);

                // ATR 계산용 데이터
                List<DailyStock> history = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                        LocalDate d = today.minusDays(19 - i);
                        history.add(DailyStock.builder()
                                        .basDt(d)
                                        .symbol(symbol)
                                        .market("KR")
                                        .highPrice(new BigDecimal("75000"))
                                        .lowPrice(new BigDecimal("70000"))
                                        .closePrice(new BigDecimal("72000"))
                                        .build());
                }
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                                eq(symbol), eq("KR"), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(history);

                when(exitRuleEvaluator.evaluate(any(ExitRuleInput.class)))
                                .thenReturn(ExitRuleResult.exit("TIME_CUT"));

                // when
                List<ExitRuleService.ExitSignal> signals = exitRuleService.getSellSignals(accountNo,
                                currentPriceBySymbol);

                // then
                assertThat(signals).hasSize(1);
                assertThat(signals.get(0).getReason()).isEqualTo("TIME_CUT");
                assertThat(signals.get(0).getSymbol()).isEqualTo(symbol);
        }

        @Test
        @DisplayName("현재가 없으면 시그널 생성 안 함")
        void getSellSignals_noCurrentPrice_noSignal() {
                // given
                String accountNo = "1234567890";
                StrategyPosition position = StrategyPosition.builder()
                                .id(1L)
                                .accountNo(accountNo)
                                .symbol("005930")
                                .market("KR")
                                .strategyType(StrategyType.SHORT_TERM)
                                .entryDt(LocalDate.now().minusDays(10))
                                .entryPrice(new BigDecimal("70000"))
                                .quantity(10)
                                .build();

                when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(accountNo))
                                .thenReturn(List.of(position));
                when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                                any(), any(), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(List.of());

                // when
                List<ExitRuleService.ExitSignal> signals = exitRuleService.getSellSignals(accountNo, null);

                // then
                assertThat(signals).isEmpty();
        }
}
