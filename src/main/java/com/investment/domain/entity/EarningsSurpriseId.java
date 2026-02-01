package com.investment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TB_EARNINGS_SURPRISE 복합 PK (SYMBOL, MARKET, REPORT_DT).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EarningsSurpriseId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private String market;
    private LocalDate reportDt;
}
