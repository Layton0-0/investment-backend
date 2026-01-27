package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 보유 종목 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountPositionDto {
    
    @NotNull
    private String symbol;
    
    @NotNull
    private String name;
    
    @NotNull
    @PositiveOrZero
    private Integer quantity;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal averagePrice;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal currentPrice;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalValue;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal profitLoss;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal profitLossRate;
    
    @NotNull
    private String currency;
    
    private LocalDateTime lastUpdated;
}
