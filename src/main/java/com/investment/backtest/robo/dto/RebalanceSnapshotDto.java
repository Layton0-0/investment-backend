package com.investment.backtest.robo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 리밸런싱 시점 스냅샷 — 일자, 자산별 비중, 현금 비중, 당일 회전율.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceSnapshotDto {

    private LocalDate date;
    /** 자산별 비중 (symbol → weight 0~1) */
    private Map<String, BigDecimal> weights;
    /** 현금 비중 (0~1) */
    private BigDecimal cashWeight;
    /** 당일 회전율 (%) */
    private BigDecimal turnoverPct;
}
