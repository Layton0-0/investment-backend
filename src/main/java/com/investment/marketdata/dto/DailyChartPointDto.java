package com.investment.marketdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일봉 차트 한 건 (TB_DAILY_STOCK 기반).
 * 프론트 DailyChartPointDto(date, open, high, low, close, volume)와 형식 맞춤.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "일봉 차트 데이터 한 건")
public class DailyChartPointDto {

    @Schema(description = "기준일 (yyyy-MM-dd)")
    private String date;

    @Schema(description = "시가")
    private BigDecimal open;

    @Schema(description = "고가")
    private BigDecimal high;

    @Schema(description = "저가")
    private BigDecimal low;

    @Schema(description = "종가")
    private BigDecimal close;

    @Schema(description = "거래량")
    private Long volume;
}
