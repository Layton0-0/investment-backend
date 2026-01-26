package com.investment.tradingportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 트레이딩 포트폴리오 종목 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingPortfolioItemDto {
    
    private String id;
    private String symbol;
    private String name;
    private BigDecimal entryPriceMin;
    private BigDecimal entryPriceMax;
    private BigDecimal stopLossPrice;
    private BigDecimal targetPrice1;
    private BigDecimal targetPrice2;
    private BigDecimal expectedReturnRate;
    private BigDecimal riskRewardRatio;
    private String technicalBasis;
    private String supplyDemandBasis;
    private String catalystFactor;
    private LocalTime buyTime;
    private LocalTime sellTime;
    private BigDecimal investmentAmount;
    private BigDecimal expectedProfit;
    private Integer ranking;
}
