package com.investment.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 대시보드용 성과 요약 DTO.
 * 기간별 수익률·MDD·Sharpe 등 리스크 요약 데이터를 카드/차트용으로 제공.
 */
@Schema(description = "대시보드 성과 요약 (총 평가액·MDD·Sharpe·VaR 등)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPerformanceSummaryDto {

    @Schema(description = "계좌 합산 현재 평가액")
    private BigDecimal totalCurrentValue;

    @Schema(description = "최대 낙폭 MDD (0~1, 예: 0.15 = 15%)")
    private BigDecimal maxMddPct;

    @Schema(description = "Sharpe 비율 (연율화). 데이터 없으면 null")
    private BigDecimal sharpeRatio;

    @Schema(description = "Sortino 비율 (연율화). 데이터 없으면 null")
    private BigDecimal sortinoRatio;

    @Schema(description = "1일 VaR 95% (%). 없으면 null")
    private BigDecimal var95Pct;

    @Schema(description = "1일 CVaR 95% (%). 없으면 null")
    private BigDecimal cvar95Pct;

    @Schema(description = "당일 손익 합계 (원). 계좌별 일일 손익 합산, 없으면 null")
    private BigDecimal dailyProfitLoss;

    @Schema(description = "리스크 수준: 낮음 / 중간 / 높음. VaR·MDD 기반")
    private String riskLevel;
}
