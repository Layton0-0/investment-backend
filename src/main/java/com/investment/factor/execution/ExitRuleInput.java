package com.investment.factor.execution;

import com.investment.strategy.domain.StrategyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 청산 규칙 평가 입력 — 포지션 상태 + 당일 시세.
 * ExitRuleEvaluator와 BacktestService에서 공통 사용.
 */
@Getter
@Builder
public class ExitRuleInput {

    private final BigDecimal entryPrice;
    private final BigDecimal trailingHigh;
    private final BigDecimal priorLow;
    private final LocalDate entryDt;
    private final StrategyType strategyType;
    private final String market;
    private final BigDecimal currentPrice;
    private final BigDecimal todayHigh;
    private final LocalDate today;
    private final int timeCutDays;
    private final BigDecimal targetReturnPct;
    private final BigDecimal atrMultiplier;
    /** RSI(14). 한국 KR 단기/스윙 RSI≥70 익절 시 사용. */
    private final BigDecimal rsi;
}
