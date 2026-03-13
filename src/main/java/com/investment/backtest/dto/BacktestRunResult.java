package com.investment.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 백테스트 실행 결과 — 메트릭·수익 곡선·거래 목록.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestRunResult {

    private LocalDate startDate;
    private LocalDate endDate;
    private String market;
    private String strategyType;
    private BigDecimal initialCapital;
    private BigDecimal finalEquity;
    private BigDecimal totalReturnPct;
    private BigDecimal cagr;
    private BigDecimal mddPct;
    private BigDecimal sharpeRatio;
    private BigDecimal sortinoRatio;
    private BigDecimal calmarRatio;
    private BigDecimal winRate;
    private BigDecimal avgWin;
    private BigDecimal avgLoss;
    private BigDecimal profitFactor;
    private int tradeCount;
    private int winningTrades;
    private int losingTrades;
    /** 전체 거래에 대한 마찰 비용 합계 (수수료+세금+슬리피지+TAF 등). 거래 없으면 0. */
    private BigDecimal totalFrictionCost;
    private List<DateEquityPoint> equityCurve;
    private List<BacktestTradeDto> trades;
}
