package com.investment.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 포트폴리오(단일 계좌) 리스크 메트릭 API 응답.
 * VaR/CVaR·Sharpe/Sortino 등. 일수익 시계열 없으면 sharpeRatio/sortinoRatio는 null.
 */
@Schema(description = "포트폴리오 리스크 메트릭 (VaR, CVaR, Sharpe, Sortino)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioRiskMetricsDto {

    @Schema(description = "계좌번호 마스킹")
    private String accountNoMasked;
    @Schema(description = "현재 평가액")
    private BigDecimal currentValue;
    @Schema(description = "MDD (0~1). 없으면 null")
    private BigDecimal mddPct;
    @Schema(description = "1일 VaR 95% (%). 단순 파라메트릭, 없으면 null")
    private BigDecimal var95Pct;
    @Schema(description = "1일 CVaR 95% (%). 단순 파라메트릭, 없으면 null")
    private BigDecimal cvar95Pct;
    @Schema(description = "Sharpe 비율 (연율화). 일수익 시계열 있으면 산출, 없으면 null")
    private BigDecimal sharpeRatio;
    @Schema(description = "Sortino 비율 (연율화). 일수익 시계열 있으면 산출, 없으면 null")
    private BigDecimal sortinoRatio;
}
