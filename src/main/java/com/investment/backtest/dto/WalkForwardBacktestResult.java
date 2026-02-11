package com.investment.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Walk-Forward 백테스트 결과 — fold별 결과·집계 메트릭.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkForwardBacktestResult {

    private LocalDate startDate;
    private LocalDate endDate;
    private String market;
    private String strategyType;
    private int trainDays;
    private int testDays;
    private int stepDays;

    /** fold 수 */
    private int foldCount;

    /** fold별 백테스트 결과 (test 구간만). */
    private List<BacktestRunResult> folds;

    /** 집계: 평균 CAGR(%) */
    private BigDecimal avgCagr;
    /** 집계: 평균 MDD(%) */
    private BigDecimal avgMddPct;
    /** 집계: 최소 Sharpe (fold 중) */
    private BigDecimal minSharpeRatio;
    /** 집계: 평균 Sharpe */
    private BigDecimal avgSharpeRatio;
    /** 집계: 평균 승률 */
    private BigDecimal avgWinRate;
    /** 집계: 평균 손익비 */
    private BigDecimal avgProfitFactor;
}
