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

    /** 국내(KR) 6자리 코드일 때 종목명, 해외는 null. 표시용. */
    private String symbolName;

    /** 시장 구분 (KR: 원화, US: 달러). 가격 통화 표시용. */
    private String market;
    
    @NotNull
    private OrderRequestDto.OrderType orderType;
    
    @NotNull
    private Integer quantity;
    
    @NotNull
    private BigDecimal price;

    /** 주당 가격 × 수량 (총 금액). 표시용. */
    private BigDecimal totalAmount;
    
    @NotNull
    private OrderStatus status;
    
    @NotNull
    private LocalDateTime orderTime;
    
    private String message;

    /** 거래 사유: 진입 시그널 유형 (파이프라인 매수 시). 예: VOLATILITY_BREAKOUT, DUAL_MOMENTUM */
    private String signalType;

    /** 거래 사유: 청산 규칙 유형 (파이프라인 매도 시). 예: ATR_TRAILING_STOP, TIME_CUT, STOP_LOSS */
    private String exitRuleType;

    /** 매매 사유 한글 평문 설명 (초보자 친화) */
    private String explanation;

    public enum OrderStatus {
        PENDING,    // 대기중
        EXECUTED,   // 체결완료
        PARTIAL,    // 부분체결
        CANCELLED,  // 취소됨
        FAILED      // 실패
    }
}
