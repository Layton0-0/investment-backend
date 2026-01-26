package com.investment.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 분석 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisResponseDto {
    
    @NotNull
    private String symbol;
    
    @NotNull
    private String recommendation; // BUY, SELL, HOLD
    
    @NotNull
    private BigDecimal confidence; // 0.0 ~ 1.0
    
    @NotNull
    private BigDecimal targetPrice;
    
    @NotNull
    private BigDecimal currentPrice;
    
    @NotNull
    private BigDecimal expectedReturn; // 예상 수익률
    
    private List<AnalysisIndicatorDto> indicators;
    
    @NotNull
    private LocalDateTime analyzedAt;
    
    private String reasoning; // 분석 근거
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalysisIndicatorDto {
        private String name;
        private String value;
        private String interpretation;
    }
}
