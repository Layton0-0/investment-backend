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
 * 주식정정취소가능주문조회 응답 DTO (미체결 주문 목록)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelableOrderDto {

    @NotNull
    private String accountNo;

    @NotNull
    private String symbol;

    private String orderNo;

    private String orderBranchNo;

    private String orderType;

    @NotNull
    @PositiveOrZero
    private Integer orderQuantity;

    @NotNull
    @PositiveOrZero
    private BigDecimal orderPrice;

    private LocalDateTime orderTime;

    @NotNull
    private String currency;
}
