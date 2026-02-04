package com.investment.factor.execution;

import com.investment.strategy.domain.StrategyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 청산 규칙 평가 — 포지션 + 당일 시세만으로 청산 여부 판단.
 * ExitRuleService와 BacktestService에서 공통 사용.
 */
@Component
public class ExitRuleEvaluator {

    @Value("${investment.pipeline.short-term-trailing-pct:3}")
    private BigDecimal shortTermTrailingPct = new BigDecimal("3");

    @Value("${investment.pipeline.medium-term-stop-loss-pct:10}")
    private BigDecimal mediumTermStopLossPct = new BigDecimal("10");

    /** 한국(KR) 단기/스윙: 진입가 대비 -5% 고정 손절 (%) */
    @Value("${investment.pipeline.short-term-kr-stop-loss-pct:5}")
    private BigDecimal shortTermKrStopLossPct = new BigDecimal("5");

    /** 한국(KR) 전저점 이탈 손절 사용 여부 */
    @Value("${investment.pipeline.prior-low-stop-kr-enabled:true}")
    private boolean priorLowStopKrEnabled = true;

    /** 한국(KR) RSI≥N 익절 임계값 (0~100) */
    @Value("${investment.pipeline.rsi-exit-threshold:70}")
    private BigDecimal rsiExitThreshold = new BigDecimal("70");

    /**
     * 주어진 입력으로 청산 여부와 사유 반환.
     *
     * @param input 포지션 + currentPrice, todayHigh, today
     * @return shouldExit true이면 청산, reason은 ExitSignal.reason에 사용
     */
    public ExitRuleResult evaluate(ExitRuleInput input) {
        if (input.getCurrentPrice() == null || input.getCurrentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return ExitRuleResult.noExit();
        }
        StrategyType term = input.getStrategyType() != null ? input.getStrategyType() : StrategyType.SHORT_TERM;

        if (term == StrategyType.LONG_TERM) {
            return ExitRuleResult.noExit();
        }

        if (term == StrategyType.SHORT_TERM) {
            // 한국(KR) 전용: -5% 고정 손절, 전저점 이탈, RSI≥70 익절
            if ("KR".equalsIgnoreCase(input.getMarket())) {
                ExitRuleResult r = evaluateKrFixedStopLoss(input);
                if (r.isShouldExit())
                    return r;
                if (priorLowStopKrEnabled) {
                    r = evaluatePriorLowStop(input);
                    if (r.isShouldExit())
                        return r;
                }
                r = evaluateRsiExit(input);
                if (r.isShouldExit())
                    return r;
            }
            ExitRuleResult r = evaluateShortTermTrailingStop(input);
            if (r.isShouldExit()) {
                return r;
            }
            // Time-Cut: 단기(SHORT_TERM)에만 적용. 중/장기 듀얼 모멘텀은 추세 훼손만 청산.
            long daysHeld = java.time.temporal.ChronoUnit.DAYS.between(input.getEntryDt(), input.getToday());
            if (input.getTimeCutDays() > 0 && daysHeld >= input.getTimeCutDays()) {
                BigDecimal returnPct = input.getCurrentPrice().subtract(input.getEntryPrice())
                        .divide(input.getEntryPrice(), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                if (input.getTargetReturnPct() != null && returnPct.compareTo(input.getTargetReturnPct()) < 0) {
                    return ExitRuleResult.exit("TIME_CUT");
                }
            }
            return ExitRuleResult.noExit();
        }

        if (term == StrategyType.MEDIUM_TERM) {
            ExitRuleResult r = evaluateMediumTermStopLoss(input);
            if (r.isShouldExit()) {
                return r;
            }
            // Time-Cut은 SHORT_TERM 전용. MEDIUM_TERM은 -10% 손절·추세 훼손만.
        }
        return ExitRuleResult.noExit();
    }

    /**
     * 단기 -3% Trailing Stop: 현재가 ≤ trailingHigh × (1 - pct/100) 시 매도.
     */
    private ExitRuleResult evaluateShortTermTrailingStop(ExitRuleInput input) {
        if (input.getTrailingHigh() == null || input.getTrailingHigh().compareTo(BigDecimal.ZERO) <= 0) {
            return ExitRuleResult.noExit();
        }
        BigDecimal threshold = input.getTrailingHigh()
                .multiply(BigDecimal.ONE.subtract(shortTermTrailingPct.movePointLeft(2)));
        if (input.getCurrentPrice().compareTo(threshold) <= 0) {
            return ExitRuleResult.exit("SHORT_TERM_TRAILING_STOP");
        }
        return ExitRuleResult.noExit();
    }

    /**
     * 중기 -10% 손절: 현재가 ≤ entryPrice × (1 - pct/100) 시 매도.
     */
    private ExitRuleResult evaluateMediumTermStopLoss(ExitRuleInput input) {
        BigDecimal threshold = input.getEntryPrice()
                .multiply(BigDecimal.ONE.subtract(mediumTermStopLossPct.movePointLeft(2)));
        if (input.getCurrentPrice().compareTo(threshold) <= 0) {
            return ExitRuleResult.exit("MEDIUM_TERM_STOP_LOSS");
        }
        return ExitRuleResult.noExit();
    }

    /** 한국(KR) 단기/스윙: 진입가 대비 -5% 고정 손절 */
    private ExitRuleResult evaluateKrFixedStopLoss(ExitRuleInput input) {
        BigDecimal threshold = input.getEntryPrice()
                .multiply(BigDecimal.ONE.subtract(shortTermKrStopLossPct.movePointLeft(2)));
        if (input.getCurrentPrice().compareTo(threshold) <= 0) {
            return ExitRuleResult.exit("KR_FIXED_STOP_LOSS");
        }
        return ExitRuleResult.noExit();
    }

    /** 한국(KR) 전저점 이탈: 현재가 < priorLow 시 매도 */
    private ExitRuleResult evaluatePriorLowStop(ExitRuleInput input) {
        if (input.getPriorLow() == null || input.getPriorLow().compareTo(BigDecimal.ZERO) <= 0) {
            return ExitRuleResult.noExit();
        }
        if (input.getCurrentPrice().compareTo(input.getPriorLow()) < 0) {
            return ExitRuleResult.exit("KR_PRIOR_LOW_STOP");
        }
        return ExitRuleResult.noExit();
    }

    /** 한국(KR) RSI≥70 익절 */
    private ExitRuleResult evaluateRsiExit(ExitRuleInput input) {
        if (input.getRsi() == null || rsiExitThreshold == null) {
            return ExitRuleResult.noExit();
        }
        if (input.getRsi().compareTo(rsiExitThreshold) >= 0) {
            return ExitRuleResult.exit("KR_RSI_EXIT");
        }
        return ExitRuleResult.noExit();
    }
}
