package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 거래 설정 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingSettingDto {
    
    @NotNull(message = "최대 투자금액은 필수입니다")
    @Positive(message = "최대 투자금액은 양수여야 합니다")
    private BigDecimal maxInvestmentAmount;
    
    @NotNull(message = "최소 투자금액은 필수입니다")
    @Positive(message = "최소 투자금액은 양수여야 합니다")
    private BigDecimal minInvestmentAmount;
    
    @NotNull(message = "기본 통화는 필수입니다")
    private String defaultCurrency;
    
    private Boolean autoTradingEnabled;
    
    private BigDecimal riskLevel; // 0.0 ~ 1.0
}
