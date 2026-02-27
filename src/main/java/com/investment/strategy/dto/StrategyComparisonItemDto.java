package com.investment.strategy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 전략 비교 화면용 DTO — (market, strategyType)별 최신 백테스트 메트릭.
 */
@Schema(description = "전략 비교 항목 (CAGR, MDD, Sharpe, 설명)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyComparisonItemDto {

    @Schema(description = "시장 (KR, US)")
    private String market;

    @Schema(description = "전략 타입 (SHORT_TERM, MEDIUM_TERM, LONG_TERM)")
    private String strategyType;

    @Schema(description = "전략 타입 설명")
    private String description;

    @Schema(description = "CAGR (%) — 거버넌스 저장 시 포함 시 반영, 미저장 시 null")
    private BigDecimal cagr;

    @Schema(description = "최대 낙폭 (%)")
    private BigDecimal mddPct;

    @Schema(description = "Sharpe 비율")
    private BigDecimal sharpeRatio;

    @Schema(description = "최근 백테스트 실행 시각")
    private Instant lastRunAt;
}
