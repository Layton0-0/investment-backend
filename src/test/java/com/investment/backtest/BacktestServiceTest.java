package com.investment.backtest;

import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.config.FrictionCostProperties;
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
    @Mock
    private FrictionCostProperties frictionCostProperties;

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
        // Required metrics (BacktestRunResult must include these)
        assertThat(result.getCagr()).isNotNull();
        assertThat(result.getMddPct()).isNotNull();
        assertThat(result.getSharpeRatio()).isNull(); // can be null with flat curve
        assertThat(result.getWinRate()).isNull();
        assertThat(result.getProfitFactor()).isNull();
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

    @Test
    @DisplayName("스트레스 시나리오 2020-03 코로나 구간 요청 시 정상 완료 (권장 없으면 거래 0건)")
    void run_stressScenario2020Covid_completesWithoutError() {
        when(positionSizingService.getRecommendations(any(LocalDate.class), eq("KR"), any(), any(BigDecimal.class)))
                .thenReturn(Collections.emptyList());

        BacktestRunRequest request = BacktestRunRequest.builder()
                .startDate(LocalDate.of(2020, 2, 24))
                .endDate(LocalDate.of(2020, 4, 30))
                .market("KR")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .build();

        BacktestRunResult result = backtestService.run(request);

        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2020, 2, 24));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2020, 4, 30));
        assertThat(result.getFinalEquity()).isEqualByComparingTo(new BigDecimal("100000000"));
        assertThat(result.getEquityCurve()).isNotEmpty();
        assertThat(result.getTradeCount()).isZero();
    }

    @Test
    @DisplayName("스트레스 시나리오 2022-01~06 금리 인상기 구간 요청 시 정상 완료 (권장 없으면 거래 0건)")
    void run_stressScenario2022RateHike_completesWithoutError() {
        when(positionSizingService.getRecommendations(any(LocalDate.class), eq("US"), any(), any(BigDecimal.class)))
                .thenReturn(Collections.emptyList());

        BacktestRunRequest request = BacktestRunRequest.builder()
                .startDate(LocalDate.of(2022, 1, 3))
                .endDate(LocalDate.of(2022, 6, 30))
                .market("US")
                .strategyType("SHORT_TERM")
                .initialCapital(new BigDecimal("100000000"))
                .build();

        BacktestRunResult result = backtestService.run(request);

        assertThat(result.getStartDate()).isEqualTo(LocalDate.of(2022, 1, 3));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.of(2022, 6, 30));
        assertThat(result.getFinalEquity()).isEqualByComparingTo(new BigDecimal("100000000"));
        assertThat(result.getEquityCurve()).isNotEmpty();
        assertThat(result.getTradeCount()).isZero();
    }

    @Test
    @DisplayName("거래 없을 때 totalFrictionCost는 0으로 노출")
    void run_noTrades_totalFrictionCostIsZero() {
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

        assertThat(result.getTradeCount()).isZero();
        assertThat(result.getTotalFrictionCost()).isNotNull().isEqualByComparingTo(BigDecimal.ZERO);
    }
}
