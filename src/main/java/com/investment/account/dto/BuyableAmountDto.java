package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * 매수가능조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyableAmountDto {
    
    @NotNull
    private String accountNo;
    
    @NotNull
    private String symbol; // 종목코드
    
    @NotNull
    @PositiveOrZero
    private BigDecimal price; // 주문가격
    
    @NotNull
    @PositiveOrZero
    private BigDecimal buyableAmount; // 매수 가능 금액 (ord_psbl_cash)
    
    @NotNull
    @PositiveOrZero
    private Integer buyableQuantity; // 매수 가능 수량 (ord_psbl_qty)
    
    @NotNull
    private String currency;
}
