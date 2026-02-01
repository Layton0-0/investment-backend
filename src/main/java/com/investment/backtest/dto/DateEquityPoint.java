package com.investment.backtest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일자별 자산(equity) 포인트 — 수익 곡선 차트용.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateEquityPoint {

    private LocalDate date;
    private BigDecimal equity;
}
