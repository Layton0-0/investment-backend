package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/**
 * 주식잔고조회_실현손익(TTTC8494R/VTTC8494R) API 응답 DTO.
 * output2 총실현손익(tot_rlzt_pfls) 및 output1 상세 목록.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceRealizedProfitLossDto {

    @NotNull
    private String accountNo;

    /** 총 실현손익 (output2 tot_rlzt_pfls) */
    private BigDecimal totalRealizedProfitLoss;

    @NotNull
    private String currency;

    /** 일별/종목별 실현손익 상세 (output1) */
    private List<RealizedProfitLossItemDto> details;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealizedProfitLossItemDto {
        /** 매매일자 (trad_dt) */
        private String tradeDate;
        /** 실현손익 (rlzt_pfls) */
        private BigDecimal realizedProfitLoss;
        /** 매수금액 (buy_amt) */
        private BigDecimal buyAmount;
        /** 매도금액 (sll_amt) */
        private BigDecimal sellAmount;
    }
}
