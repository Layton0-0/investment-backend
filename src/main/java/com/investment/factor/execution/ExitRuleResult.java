package com.investment.factor.execution;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 청산 규칙 평가 결과 — 청산 여부와 사유.
 */
@Getter
@AllArgsConstructor
public class ExitRuleResult {

    private final boolean shouldExit;
    /** 청산 시 사유: SHORT_TERM_TRAILING_STOP, MEDIUM_TERM_STOP_LOSS, TIME_CUT 등 */
    private final String reason;

    public static ExitRuleResult noExit() {
        return new ExitRuleResult(false, null);
    }

    public static ExitRuleResult exit(String reason) {
        return new ExitRuleResult(true, reason);
    }
}
