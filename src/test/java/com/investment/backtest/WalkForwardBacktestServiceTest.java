package com.investment.backtest;

import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.backtest.dto.WalkForwardBacktestRequest;
import com.investment.backtest.dto.WalkForwardBacktestResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WalkForwardBacktestService")
class WalkForwardBacktestServiceTest {

    @Mock
    private BacktestService backtestService;

    @InjectMocks
    private WalkForwardBacktestService walkForwardBacktestService;

    @Test
    @DisplayName("한 개 fold: test 구간 1회 백테스트 후 집계 반환")
    void run_singleFold_returnsAggregatedResult() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);
        WalkForwardBacktestRequest request = WalkForwardBacktestRequest.builder()
                .startDate(start)
                .endDate(end)
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .trainDays(252)
                .testDays(63)
                .stepDays(63)
                .build();

        when(backtestService.run(any(BacktestRunRequest.class))).thenAnswer(inv -> {
            BacktestRunRequest req = inv.getArgument(0);
            return BacktestRunResult.builder()
                    .startDate(req.getStartDate())
                    .endDate(req.getEndDate())
                    .cagr(new BigDecimal("5.0"))
                    .mddPct(new BigDecimal("-3.0"))
                    .sharpeRatio(new BigDecimal("1.2"))
                    .winRate(new BigDecimal("0.6"))
                    .profitFactor(new BigDecimal("1.5"))
                    .build();
        });

        WalkForwardBacktestResult result = walkForwardBacktestService.run(request);

        assertThat(result.getFoldCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.getFolds()).hasSize(result.getFoldCount());
        assertThat(result.getAvgCagr()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(result.getAvgMddPct()).isEqualByComparingTo(new BigDecimal("-3.0"));
        assertThat(result.getMinSharpeRatio()).isEqualByComparingTo(new BigDecimal("1.2"));
        assertThat(result.getAvgSharpeRatio()).isEqualByComparingTo(new BigDecimal("1.2"));
    }

    @Test
    @DisplayName("startDate > endDate 이면 IllegalArgumentException")
    void run_invalidDateRange_throws() {
        WalkForwardBacktestRequest request = WalkForwardBacktestRequest.builder()
                .startDate(LocalDate.of(2024, 12, 1))
                .endDate(LocalDate.of(2024, 1, 1))
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .build();

        assertThatThrownBy(() -> walkForwardBacktestService.run(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startDate must be <= endDate");
    }

    @Test
    @DisplayName("test 구간이 1개도 없으면 IllegalArgumentException")
    void run_noTestWindow_throws() {
        WalkForwardBacktestRequest request = WalkForwardBacktestRequest.builder()
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 6, 30))
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .trainDays(252)
                .testDays(63)
                .build();

        assertThatThrownBy(() -> walkForwardBacktestService.run(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one test window");
    }

    @Test
    @DisplayName("각 fold마다 BacktestService.run 호출 시 test 구간만 전달")
    void run_invokesBacktestWithTestWindowOnly() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 6, 1);
        WalkForwardBacktestRequest request = WalkForwardBacktestRequest.builder()
                .startDate(start)
                .endDate(end)
                .market("US")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("50000000"))
                .trainDays(60)
                .testDays(21)
                .stepDays(21)
                .build();

        when(backtestService.run(any(BacktestRunRequest.class))).thenAnswer(inv -> {
            BacktestRunRequest req = inv.getArgument(0);
            return BacktestRunResult.builder()
                    .startDate(req.getStartDate())
                    .endDate(req.getEndDate())
                    .cagr(BigDecimal.ZERO)
                    .mddPct(BigDecimal.ZERO)
                    .build();
        });

        walkForwardBacktestService.run(request);

        ArgumentCaptor<BacktestRunRequest> captor = ArgumentCaptor.forClass(BacktestRunRequest.class);
        verify(backtestService, atLeastOnce()).run(captor.capture());
        List<BacktestRunRequest> calls = captor.getAllValues();
        assertThat(calls).isNotEmpty();
        BacktestRunRequest first = calls.get(0);
        assertThat(first.getStartDate()).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(first.getEndDate()).isEqualTo(LocalDate.of(2024, 3, 21));
        assertThat(first.getMarket()).isEqualTo("US");
        assertThat(first.getInitialCapital()).isEqualByComparingTo(new BigDecimal("50000000"));
    }
}
