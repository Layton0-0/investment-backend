package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 보유 포지션 목록용 DTO (자동투자 현황 표시).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenPositionItemDto {

    private Long positionId;
    private String symbol;
    private String market;
    private int quantity;
    private BigDecimal entryPrice;
    private LocalDate entryDt;
}
