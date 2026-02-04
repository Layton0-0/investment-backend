package com.investment.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 백테스트 단일 거래 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestTradeDto {

    private String symbol;
    private String market;
    private String strategyType;
    private LocalDate entryDt;
    private LocalDate exitDt;
    private BigDecimal entryPrice;
    private BigDecimal exitPrice;
    private int quantity;
    private BigDecimal pnl;
    private BigDecimal pnlPct;
    /** 거래당 마찰 비용 합계 (수수료+세금+슬리피지+TAF 등) */
    private BigDecimal totalFrictionCost;
    private String exitReason;
}
