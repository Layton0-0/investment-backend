package com.investment.backtest;

import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import com.investment.factor.execution.ExitRuleEvaluator;
import com.investment.factor.service.PositionSizingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BacktestService")
class BacktestServiceTest {

    @Mock
    private PositionSizingService positionSizingService;
    @Mock
    private ExitRuleEvaluator exitRuleEvaluator;
    @Mock
    private DailyStockRepository dailyStockRepository;

    @InjectMocks
    private BacktestService backtestService;

    @Test
    @DisplayName("권장 포지션 없으면 최종 자산 = 초기 자산, 거래 0건")
    void run_noRecommendations_finalEquityEqualsInitial() {
        when(positionSizingService.getRecommendations(any(LocalDate.class), eq("KR"), any(), any(BigDecimal.class)))
                .thenReturn(Collections.emptyList());

        BacktestRunRequest request = BacktestRunRequest.builder()
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 1, 10))
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .build();

        BacktestRunResult result = backtestService.run(request);

        assertThat(result.getFinalEquity()).isEqualByComparingTo(new BigDecimal("100000000"));
        assertThat(result.getTradeCount()).isZero();
        assertThat(result.getTrades()).isEmpty();
        assertThat(result.getEquityCurve()).isNotEmpty();
        assertThat(result.getStartDate()).isEqualTo(request.getStartDate());
        assertThat(result.getEndDate()).isEqualTo(request.getEndDate());
        assertThat(result.getMarket()).isEqualTo("KR");
    }

    @Test
    @DisplayName("startDate > endDate 이면 IllegalArgumentException")
    void run_invalidDateRange_throws() {
        BacktestRunRequest request = BacktestRunRequest.builder()
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 6))
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .build();

        try {
            backtestService.run(request);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("startDate");
        }
    }

    @Test
    @DisplayName("initialCapital <= 0 이면 IllegalArgumentException")
    void run_invalidCapital_throws() {
        BacktestRunRequest request = BacktestRunRequest.builder()
                .startDate(LocalDate.of(2025, 1, 6))
                .endDate(LocalDate.of(2025, 1, 10))
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(BigDecimal.ZERO)
                .build();

        try {
            backtestService.run(request);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("initialCapital");
        }
    }
}
