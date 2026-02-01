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
    private final LocalDate entryDt;
    private final StrategyType strategyType;
    private final BigDecimal currentPrice;
    private final BigDecimal todayHigh;
    private final LocalDate today;
    private final int timeCutDays;
    private final BigDecimal targetReturnPct;
    private final BigDecimal atrMultiplier;
}
