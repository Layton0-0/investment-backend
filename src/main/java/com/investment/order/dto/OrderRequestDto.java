package com.investment.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 주문 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {
    
    @NotBlank(message = "계좌번호는 필수입니다")
    private String accountNo;
    
    @NotBlank(message = "종목코드는 필수입니다")
    private String symbol;
    
    @NotNull(message = "주문유형은 필수입니다")
    private OrderType orderType;
    
    @NotNull(message = "주문수량은 필수입니다")
    @Positive(message = "주문수량은 양수여야 합니다")
    private Integer quantity;
    
    @NotNull(message = "주문가격은 필수입니다")
    @Positive(message = "주문가격은 양수여야 합니다")
    private BigDecimal price;
    
    public enum OrderType {
        BUY,    // 매수
        SELL    // 매도
    }
}
