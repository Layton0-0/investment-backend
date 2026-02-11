package com.investment.tradingportfolio.dto;

import com.investment.core.engine.portfolio.Rebalancer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 리밸런싱 제안 API 응답 (목표 비중 대비 매수/매도 제안 목록).
 */
@Schema(description = "리밸런싱 제안 목록")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalanceSuggestionsDto {

    @Schema(description = "시장 (KR/US)")
    private String market;
    @Schema(description = "계좌 평가총액 (리밸런싱 기준)")
    private BigDecimal totalValue;
    @Schema(description = "제안 건 목록")
    private List<RebalanceItemDto> items;

    @Schema(description = "리밸런싱 1건")
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RebalanceItemDto {
        private String symbol;
        private String side;  // BUY | SELL
        private BigDecimal quantity;
        private BigDecimal notional;
    }

    public static RebalanceSuggestionsDto fromRebalanceItems(String market, BigDecimal totalValue,
                                                             List<Rebalancer.RebalanceItem> items) {
        List<RebalanceItemDto> list = items.stream()
                .map(i -> RebalanceItemDto.builder()
                        .symbol(i.symbol())
                        .side(i.side())
                        .quantity(i.quantity())
                        .notional(i.notional())
                        .build())
                .collect(Collectors.toList());
        return RebalanceSuggestionsDto.builder()
                .market(market)
                .totalValue(totalValue)
                .items(list)
                .build();
    }
}
