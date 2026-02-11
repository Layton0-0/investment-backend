package com.investment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * TB_GOVERNANCE_HALT 복합 PK (MARKET, STRATEGY_TYPE).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GovernanceHaltId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String market;
    private String strategyType;
}
