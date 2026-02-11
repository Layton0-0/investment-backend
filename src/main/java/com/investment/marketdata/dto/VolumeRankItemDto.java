package com.investment.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 거래량/거래대금 순위 API 응답 항목.
 * 한국투자증권 순위분석 API(MCP volume_rank 등) 스펙에 맞게 필드명 조정.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolumeRankItemDto {

    private String symbol;
    private String name;
    private Long volume;
    private BigDecimal amount;
    private String marketDiv;
    private Integer rank;
}
