package com.investment.backtest;

import com.investment.strategy.domain.StrategyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 백테스트용 가상 포지션 (in-memory).
 */
@Getter
@Builder
public class BacktestPosition {

    private final String symbol;
    private final String market;
    private final StrategyType strategyType;
    private final LocalDate entryDt;
    private final BigDecimal entryPrice;
    private final int quantity;
    /** 고점 대비 Trailing Stop 갱신용 (가변) */
    private BigDecimal trailingHigh;
    /** 진입 후 최저가(전저점). 한국 KR 전저점 이탈 손절용 */
    private BigDecimal priorLow;
    private final int timeCutDays;
    private final BigDecimal targetReturnPct;
    private final BigDecimal atrMultiplier;

    public void updateTrailingHigh(BigDecimal high) {
        if (high != null && (trailingHigh == null || high.compareTo(trailingHigh) > 0)) {
            this.trailingHigh = high;
        }
    }

    public void updatePriorLow(BigDecimal low) {
        if (low != null && low.compareTo(BigDecimal.ZERO) > 0
                && (priorLow == null || low.compareTo(priorLow) < 0)) {
            this.priorLow = low;
        }
    }
}
