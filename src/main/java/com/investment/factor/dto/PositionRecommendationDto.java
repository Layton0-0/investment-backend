package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 포지션 권장 DTO (3단계 자금 관리 산출물).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionRecommendationDto {

    private LocalDate basDt;
    private String symbol;
    private String market;
    private BigDecimal recommendedAmt;
    private long recommendedQty;
    private String method;
    private BigDecimal entryPrice;
    private BigDecimal stopLoss;
}
