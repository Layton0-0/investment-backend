package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * 계좌 잔고 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceDto {
    
    @NotNull
    private String accountNo;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalBalance;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal availableBalance;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal investedAmount;
    
    @NotNull
    private String currency;
}
