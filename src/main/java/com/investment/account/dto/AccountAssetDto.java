package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * 투자계좌자산현황조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAssetDto {
    
    @NotNull
    private String accountNo;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalAssetValue; // 총 자산 (tot_evlu_amt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal deposit; // 예수금 (dnca_tot_amt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal stockValue; // 주식 평가금액 (scts_evlu_amt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalProfitLoss; // 총 손익 (evlu_pfls_smtl_amt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalProfitLossRate; // 총 손익률 (evlu_pfls_rt)
    
    @NotNull
    @PositiveOrZero
    private BigDecimal orderableCash; // 주문가능금액 (ord_psbl_cash)
    
    @NotNull
    private String currency;
}
