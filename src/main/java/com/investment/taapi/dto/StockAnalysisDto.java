package com.investment.taapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 주식 분석 결과 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAnalysisDto {
    
    private String symbol;
    private String name;
    private BigDecimal currentPrice;
    private BigDecimal rsi;
    private BigDecimal macd;
    private BigDecimal macdSignal;
    private BigDecimal macdHist;
    private BigDecimal ema20;
    private BigDecimal ema60;
    private BigDecimal ema120;
    private BigDecimal vwap;
    private BigDecimal bbUpper;
    private BigDecimal bbMiddle;
    private BigDecimal bbLower;
    private BigDecimal atr;
    private BigDecimal volume;
    private BigDecimal volumeChange; // 거래량 변화율 (%)
    private boolean goldenCross; // 골든크로스 여부
    private boolean breakout; // 돌파 여부
    private BigDecimal expectedReturn; // 예상 수익률
    private BigDecimal riskRewardRatio; // 리스크/리워드 비율
}
