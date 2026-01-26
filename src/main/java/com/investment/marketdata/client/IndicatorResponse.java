package com.investment.marketdata.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 지표 응답 DTO (공통)
 * 다양한 시장 데이터 제공자의 응답을 통일된 형태로 변환합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndicatorResponse {
    
    private BigDecimal value;
    private BigDecimal[] values;
    
    // MACD 관련
    private BigDecimal valueMacd;
    private BigDecimal valueMacdSignal;
    private BigDecimal valueMacdHist;
    
    // Bollinger Bands 관련
    private BigDecimal valueUpperBand;
    private BigDecimal valueMiddleBand;
    private BigDecimal valueLowerBand;
    
    // ATR 관련
    private BigDecimal valueAtr;
    
    // 에러 메시지
    private String error;
    
    /**
     * 값이 유효한지 확인
     */
    public boolean hasValue() {
        return value != null || (values != null && values.length > 0);
    }
}
