package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * 계좌 잔고 조회 응답 DTO
 * 한국투자증권 API 응답 필드를 포함합니다.
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
    private BigDecimal totalBalance; // 총 잔고 (기존 호환성 유지)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal availableBalance; // 주문가능금액 (기존 호환성 유지)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal investedAmount; // 투자금액 (기존 호환성 유지)
    
    @NotNull
    private String currency;
    
    // 한국투자증권 API 응답 필드
    private BigDecimal deposit; // 예수금 (dnca_tot_amt)
    private BigDecimal orderableCash; // 주문가능금액 (ord_psbl_cash)
    private BigDecimal totalAssetValue; // 총 평가금액 (tot_evlu_amt)
    private BigDecimal totalProfitLoss; // 총 손익 (evlu_pfls_smtl_amt)
    private BigDecimal totalProfitLossRate; // 총 손익률 (evlu_pfls_rt)
}
