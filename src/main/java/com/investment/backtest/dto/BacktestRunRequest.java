package com.investment.backtest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 백테스트 실행 요청.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BacktestRunRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private String market;

    /** SHORT_TERM, MEDIUM_TERM, LONG_TERM */
    @NotNull
    private String strategyType;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal initialCapital;
}
