package com.investment.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주문 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    
    @NotNull
    private String orderId;
    
    @NotNull
    private String accountNo;
    
    @NotNull
    private String symbol;
    
    @NotNull
    private OrderRequestDto.OrderType orderType;
    
    @NotNull
    private Integer quantity;
    
    @NotNull
    private BigDecimal price;
    
    @NotNull
    private OrderStatus status;
    
    @NotNull
    private LocalDateTime orderTime;
    
    private String message;
    
    public enum OrderStatus {
        PENDING,    // 대기중
        EXECUTED,   // 체결완료
        PARTIAL,    // 부분체결
        CANCELLED,  // 취소됨
        FAILED      // 실패
    }
}
