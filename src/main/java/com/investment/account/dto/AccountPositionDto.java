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
 * 한국투자증권 API 응답 필드를 포함합니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountPositionDto {
    
    @NotNull
    private String symbol; // 종목코드 (pdno)
    
    @NotNull
    private String name; // 종목명 (prdt_name)
    
    @NotNull
    @PositiveOrZero
    private Integer quantity; // 보유수량 (hldg_qty)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal averagePrice; // 평균매수가 (pchs_avg_pric)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal currentPrice; // 현재가 (prpr)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalValue; // 평가금액 (evlu_amt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal profitLoss; // 평가손익 (evlu_pfls_amt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal profitLossRate; // 평가손익률 (evlu_pfls_rt)
    
    @NotNull
    private String currency;

    /** 시장 구분: KR(국내), US(미국). 한국투자증권 API 응답의 거래소 구분 또는 해외 포함 여부로 설정. 미존재 시 KR. */
    private String market;

    private LocalDateTime lastUpdated;
    
    // 한국투자증권 API 응답 필드
    private BigDecimal purchaseAmount; // 매입금액 (pchs_amt)
    private BigDecimal evaluationAmount; // 평가금액 (evlu_amt)
    private BigDecimal evaluationProfitLoss; // 평가손익 (evlu_pfls_amt)
    private BigDecimal evaluationProfitLossRate; // 평가손익률 (evlu_pfls_rt)
    private Integer holdingQuantity; // 보유수량 (hldg_qty)
}
