package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 기간별매매손익현황조회(TTTC8709R/VTTC8709R) API 응답 DTO.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeriodProfitLossStatusDto {

    @NotNull
    private String accountNo;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    /** 총 실현손익 등 요약 (output2) */
    private BigDecimal totalRealizedProfitLoss;

    @NotNull
    private String currency;

    /** 종목별/일별 매매손익 현황 목록 (output1) */
    private List<ProfitLossStatusItemDto> items;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfitLossStatusItemDto {
        private String symbol;
        private String symbolName;
        private BigDecimal realizedProfitLoss;
        private BigDecimal buyAmount;
        private BigDecimal sellAmount;
    }
}
