package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * 매도가능수량조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellableQuantityDto {
    
    @NotNull
    private String accountNo;
    
    @NotNull
    private String symbol; // 종목코드
    
    @NotNull
    @PositiveOrZero
    private Integer sellableQuantity; // 매도 가능 수량 (ord_psbl_qty)
    
    @NotNull
    @PositiveOrZero
    private Integer holdingQuantity; // 보유 수량 (hldg_qty)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal averagePrice; // 평균 매수가
    
    @NotNull
    private String currency;
}
