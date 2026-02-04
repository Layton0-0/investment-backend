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
        ReflectionTestUtils.setField(exitRuleEvaluator, "shortTermKrStopLossPct", new BigDecimal("5"));
        ReflectionTestUtils.setField(exitRuleEvaluator, "priorLowStopKrEnabled", true);
        ReflectionTestUtils.setField(exitRuleEvaluator, "rsiExitThreshold", new BigDecimal("70"));
    }

    @Test
    @DisplayName("SHORT_TERM - 현재가가 trailingHigh 대비 -3% 이하이면 청산")
    void evaluate_shortTerm_trailingStop_exit() {
        // 80000 * 0.97 = 77600. 현재가 > 77600 이면 청산 안 함
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("70000"))
                .trailingHigh(new BigDecimal("80000"))
                .priorLow(null)
                .entryDt(LocalDate.now().minusDays(5))
                .strategyType(StrategyType.SHORT_TERM)
                .market("US")
                .currentPrice(new BigDecimal("78000"))
                .todayHigh(new BigDecimal("80000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        assertThat(exitRuleEvaluator.evaluate(input).isShouldExit()).isFalse();

        // 현재가 <= 77600 이면 청산
        input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("70000"))
                .trailingHigh(new BigDecimal("80000"))
                .priorLow(null)
                .entryDt(LocalDate.now().minusDays(5))
                .strategyType(StrategyType.SHORT_TERM)
                .market("US")
                .currentPrice(new BigDecimal("77600"))
                .todayHigh(new BigDecimal("80000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("SHORT_TERM_TRAILING_STOP");
    }

    @Test
    @DisplayName("SHORT_TERM KR - 진입가 대비 -5% 이하이면 KR_FIXED_STOP_LOSS 청산")
    void evaluate_shortTerm_kr_fixedStopLoss_exit() {
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("100000"))
                .trailingHigh(new BigDecimal("102000"))
                .priorLow(new BigDecimal("99000"))
                .entryDt(LocalDate.now().minusDays(2))
                .strategyType(StrategyType.SHORT_TERM)
                .market("KR")
                .currentPrice(new BigDecimal("94000"))
                .todayHigh(new BigDecimal("95000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("KR_FIXED_STOP_LOSS");
    }

    @Test
    @DisplayName("SHORT_TERM KR - 현재가가 전저점 이탈 시 KR_PRIOR_LOW_STOP 청산")
    void evaluate_shortTerm_kr_priorLowStop_exit() {
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("100000"))
                .trailingHigh(new BigDecimal("105000"))
                .priorLow(new BigDecimal("97000"))
                .entryDt(LocalDate.now().minusDays(3))
                .strategyType(StrategyType.SHORT_TERM)
                .market("KR")
                .currentPrice(new BigDecimal("96000"))
                .todayHigh(new BigDecimal("98000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("KR_PRIOR_LOW_STOP");
    }

    @Test
    @DisplayName("MEDIUM_TERM - 현재가가 진입가 대비 -10% 이하이면 청산")
    void evaluate_mediumTerm_stopLoss_exit() {
        // 100000 * 0.9 = 90000. 현재가 <= 90000 이면 청산
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("100000"))
                .trailingHigh(new BigDecimal("105000"))
                .priorLow(null)
                .entryDt(LocalDate.now().minusDays(3))
                .strategyType(StrategyType.MEDIUM_TERM)
                .market("US")
                .currentPrice(new BigDecimal("89000"))
                .todayHigh(new BigDecimal("92000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("MEDIUM_TERM_STOP_LOSS");
    }

    @Test
    @DisplayName("SHORT_TERM - Time-Cut: N일 경과 후 목표 수익률 미도달 시 TIME_CUT 청산")
    void evaluate_shortTerm_timeCut_exit() {
        // 6일 경과, timeCutDays=5, 목표 3%, 현재 수익률 약 0.7% → TIME_CUT
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("70000"))
                .trailingHigh(new BigDecimal("72000"))
                .priorLow(null)
                .entryDt(LocalDate.now().minusDays(6))
                .strategyType(StrategyType.SHORT_TERM)
                .market("US")
                .currentPrice(new BigDecimal("70500"))
                .todayHigh(new BigDecimal("71000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        ExitRuleResult result = exitRuleEvaluator.evaluate(input);
        assertThat(result.isShouldExit()).isTrue();
        assertThat(result.getReason()).isEqualTo("TIME_CUT");
    }

    @Test
    @DisplayName("MEDIUM_TERM - Time-Cut 미적용 (추세 훼손/-10% 손절만)")
    void evaluate_mediumTerm_noTimeCut() {
        // 6일 경과, 목표 미달이어도 MEDIUM_TERM은 Time-Cut 평가 안 함
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("70000"))
                .trailingHigh(new BigDecimal("72000"))
                .priorLow(null)
                .entryDt(LocalDate.now().minusDays(6))
                .strategyType(StrategyType.MEDIUM_TERM)
                .market("US")
                .currentPrice(new BigDecimal("70500"))
                .todayHigh(new BigDecimal("71000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        assertThat(exitRuleEvaluator.evaluate(input).isShouldExit()).isFalse();
    }

    @Test
    @DisplayName("LONG_TERM - 청산 안 함")
    void evaluate_longTerm_noExit() {
        ExitRuleInput input = ExitRuleInput.builder()
                .entryPrice(new BigDecimal("100000"))
                .trailingHigh(new BigDecimal("90000"))
                .priorLow(null)
                .entryDt(LocalDate.now().minusDays(30))
                .strategyType(StrategyType.LONG_TERM)
                .market("US")
                .currentPrice(new BigDecimal("80000"))
                .todayHigh(new BigDecimal("85000"))
                .today(LocalDate.now())
                .timeCutDays(5)
                .targetReturnPct(new BigDecimal("3.0"))
                .atrMultiplier(new BigDecimal("2.0"))
                .rsi(null)
                .build();
        assertThat(exitRuleEvaluator.evaluate(input).isShouldExit()).isFalse();
    }
}
