package com.investment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TB_SIGNAL_SCORE 복합 PK (BAS_DT, SYMBOL, FACTOR_TYPE).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SignalScoreId implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate basDt;
    private String symbol;
    private String factorType;
}
