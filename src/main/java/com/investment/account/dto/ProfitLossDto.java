package com.investment.account.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 기간별손익조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitLossDto {
    
    @NotNull
    private String accountNo;
    
    @NotNull
    private LocalDate startDate;
    
    @NotNull
    private LocalDate endDate;
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalProfitLoss; // 총 손익
    
    @NotNull
    @PositiveOrZero
    private BigDecimal totalProfitLossRate; // 총 손익률
    
    @NotNull
    @PositiveOrZero
    private BigDecimal realizedProfitLoss; // 실현 손익
    
    @NotNull
    @PositiveOrZero
    private BigDecimal unrealizedProfitLoss; // 미실현 손익
    
    private List<DailyProfitLossDto> dailyProfitLossList; // 일별 손익 목록
    
    @NotNull
    private String currency;
    
    /**
     * 일별 손익 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyProfitLossDto {
        
        @NotNull
        private LocalDate date;
        
        @NotNull
        @PositiveOrZero
        private BigDecimal profitLoss; // 일별 손익
        
        @NotNull
        @PositiveOrZero
        private BigDecimal profitLossRate; // 일별 손익률
    }
}
