package com.investment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TB_DAILY_STOCK 복합 PK (BAS_DT, SYMBOL).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DailyStockId implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate basDt;
    private String symbol;
}
