package com.investment.factor.execution;

import com.investment.strategy.domain.StrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExitRuleEvaluator")
class ExitRuleEvaluatorTest {

    private ExitRuleEvaluator exitRuleEvaluator;

    @BeforeEach
    void setUp() {
        exitRuleEvaluator = new ExitRuleEvaluator();
        ReflectionTestUtils.setField(exitRuleEvaluator, "shortTermTrailingPct", new BigDecimal("3"));
        ReflectionTestUtils.setField(exitRuleEvaluator, "mediumTermStopLossPct", new BigDecimal("10"));
    }

    @Test
    @DisplayName("SHORT_TERM - 현재가가 trailingHigh 대비 -3% 이하이면 청산")
    void evaluate_shortTerm_trailingStop_exit() {
        // 80000 * 0.97 = 77600. 현재가 > 77600 이면 청산 안 함
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("70000"))
                .trailingHigh(new BigDecimal("80000"))
                .entryDt(LocalDate.now().minusDays(5))
                .strategyType(StrategyType.SHORT_TERM)
                .currentPrice(new BigDecimal("78000"))
                .todayHigh(new BigDecimal("80000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .build();
        assertThat(exitRuleEvaluator.evaluate(input).isShouldExit()).isFalse();

        // 현재가 <= 77600 이면 청산
        input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("70000"))
                .trailingHigh(new BigDecimal("80000"))
                .entryDt(LocalDate.now().minusDays(5))
                .strategyType(StrategyType.SHORT_TERM)
                .currentPrice(new BigDecimal("77600"))
                .todayHigh(new BigDecimal("80000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("SHORT_TERM_TRAILING_STOP");
    }

    @Test
    @DisplayName("MEDIUM_TERM - 현재가가 진입가 대비 -10% 이하이면 청산")
    void evaluate_mediumTerm_stopLoss_exit() {
        // 100000 * 0.9 = 90000. 현재가 <= 90000 이면 청산
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("100000"))
                .trailingHigh(new BigDecimal("105000"))
                .entryDt(LocalDate.now().minusDays(3))
                .strategyType(StrategyType.MEDIUM_TERM)
                .currentPrice(new BigDecimal("89000"))
                .todayHigh(new BigDecimal("92000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("MEDIUM_TERM_STOP_LOSS");
    }

    @Test
    @DisplayName("LONG_TERM - 청산 안 함")
    void evaluate_longTerm_noExit() {
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("100000"))
                .trailingHigh(new BigDecimal("90000"))
                .entryDt(LocalDate.now().minusDays(30))
                .strategyType(StrategyType.LONG_TERM)
                .currentPrice(new BigDecimal("80000"))
                .todayHigh(new BigDecimal("85000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .build();
        assertThat(exitRuleEvaluator.evaluate(input).isShouldExit()).isFalse();
    }
}
