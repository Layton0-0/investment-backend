package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 리스크 리포트 요약 응답 DTO.
 * 킬스위치·일일 손실 한도·리스크 게이트·계좌별 요약.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskSummaryDto {

    /** Kill Switch: true면 전체 주문 차단 */
    private boolean killSwitchActive;
    /** 리스크 게이트 사용 여부 */
    private boolean regimeGateEnabled;
    /** 리스크 게이트 적용 시 신규 매수 허용 여부 (현재 평가 기준) */
    private boolean riskGateAllowsNewBuy;
    /** 리스크 게이트 적용 시 비중 배율 (0~1) */
    private BigDecimal riskGateSizeMultiplier;
    /** 계좌별 리스크 요약 (현재 사용자 소유 계좌만) */
    private List<RiskAccountSummaryDto> accounts;
    /** 계좌 합산 현재 평가액 (노출 합계). 실데이터 */
    private BigDecimal totalCurrentValue;
    /** 계좌 중 최대 MDD (0~1). 실데이터, 없으면 null */
    private BigDecimal maxMddPct;
    /** 1일 VaR 95% (포트폴리오 대비 손실 가능 비율, %). 단순 파라메트릭 추정, 없으면 null */
    private BigDecimal var95Pct;
    /** 1일 CVaR 95% (Expected Shortfall, %). 단순 파라메트릭 추정, 없으면 null */
    private BigDecimal cvar95Pct;
}
