package com.investment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * TB_ORDER_FLOW 복합 PK (BAS_DT, SYMBOL, MARKET).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OrderFlowId implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate basDt;
    private String symbol;
    private String market;
}
