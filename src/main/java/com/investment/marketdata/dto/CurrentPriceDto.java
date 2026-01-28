package com.investment.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 주식 현재가 정보 DTO
 * 
 * 한국투자증권 API의 주식현재가 조회 API 응답을 담는 DTO입니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentPriceDto {
    
    /**
     * 종목 코드 (6자리)
     */
    private String symbol;
    
    /**
     * 종목명
     */
    private String name;
    
    /**
     * 현재가
     */
    private BigDecimal currentPrice;
    
    /**
     * 전일 대비 등락률 (%)
     */
    private BigDecimal changeRate;
    
    /**
     * 전일 대비 등락액
     */
    private BigDecimal changeAmount;
    
    /**
     * 전일 종가
     */
    private BigDecimal previousClose;
    
    /**
     * 시가
     */
    private BigDecimal openPrice;
    
    /**
     * 고가
     */
    private BigDecimal highPrice;
    
    /**
     * 저가
     */
    private BigDecimal lowPrice;
    
    /**
     * 거래량
     */
    private Long volume;
    
    /**
     * 거래대금
     */
    private BigDecimal tradingValue;
    
    /**
     * 시가총액
     */
    private BigDecimal marketCap;
    
    /**
     * 상장주식수
     */
    private Long listedShares;
    
    /**
     * 조회 시각
     */
    private LocalDateTime queriedAt;
}
