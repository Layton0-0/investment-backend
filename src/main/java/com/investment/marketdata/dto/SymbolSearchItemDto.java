package com.investment.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 종목 통합 검색 결과 한 건 (코드, 명칭, 시장).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolSearchItemDto {

    private String symbol;
    private String name;
    private String market;
}
