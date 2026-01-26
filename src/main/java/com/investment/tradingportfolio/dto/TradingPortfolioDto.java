package com.investment.tradingportfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 트레이딩 포트폴리오 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingPortfolioDto {
    
    private String id;
    private LocalDate tradingDate;
    private String marketSummary;
    private String topSector1;
    private String topSector2;
    private String topSector3;
    private String riskManagementStrategy;
    private BigDecimal positionSize;
    private List<TradingPortfolioItemDto> items;
}
