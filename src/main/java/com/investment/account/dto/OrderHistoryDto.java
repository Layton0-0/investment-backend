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
 * 주문체결조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderHistoryDto {
    
    @NotNull
    private String accountNo;
    
    @NotNull
    private String symbol; // 종목코드
    
    private String orderNo; // 주문번호
    
    private String orderType; // 주문구분 (BUY/SELL)
    
    @NotNull
    @PositiveOrZero
    private Integer orderQuantity; // 주문수량
    
    @NotNull
    @PositiveOrZero
    private BigDecimal orderPrice; // 주문가격
    
    @NotNull
    @PositiveOrZero
    private Integer executedQuantity; // 체결수량
    
    @NotNull
    @PositiveOrZero
    private BigDecimal executedPrice; // 체결가격
    
    private String orderStatus; // 주문상태
    
    private LocalDateTime orderTime; // 주문시간
    
    private LocalDateTime executedTime; // 체결시간
    
    @NotNull
    private String currency;
}
