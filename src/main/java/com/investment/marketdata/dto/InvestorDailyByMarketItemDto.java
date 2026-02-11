package com.investment.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 시장별 투자자매매동향(일별) API 응답 항목.
 * 한국투자증권 MCP inquire_investor_daily_by_market 스펙에 맞게 필드명 조정.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestorDailyByMarketItemDto {

    private LocalDate stckBsopDate;
    private String prsnNtbyQty;
    private String frgnNtbyQty;
    private String orgnNtbyQty;
    private BigDecimal prsnNtbyAmt;
    private BigDecimal frgnNtbyAmt;
    private BigDecimal orgnNtbyAmt;
}
