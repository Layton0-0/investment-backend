package com.investment.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일별 OHLCV 한 시점 (예측 요청 series용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyPricePoint {

    private LocalDate date;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;
}
