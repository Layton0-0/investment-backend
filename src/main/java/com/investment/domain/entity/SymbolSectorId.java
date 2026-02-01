package com.investment.domain.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * TB_SYMBOL_SECTOR 복합 PK (SYMBOL, MARKET, SECTOR_CODE).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SymbolSectorId implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private String market;
    private String sectorCode;
}
