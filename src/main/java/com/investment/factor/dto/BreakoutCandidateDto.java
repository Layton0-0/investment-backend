package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 장중 변동성 돌파 진입 후보 (P2).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreakoutCandidateDto {

    private String symbol;
    private String market;
    private BigDecimal currentPrice;
    private BigDecimal targetPrice;
    private long recommendedQty;
}
