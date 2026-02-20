package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 리스크 한도 설정 요약 (application.yml / RiskProperties 기반).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLimitsDto {

    /** 레짐 게이트 사용 여부 */
    private boolean regimeGateEnabled;
    /** VIX 임계값. 초과 시 고변동성으로 간주 */
    private BigDecimal vixThreshold;
    /** 고변동성 시 신규 매수 비중 축소 비율 (%) */
    private BigDecimal reduceSizeOnHighVolPct;
    /** 일일 손실 한도 (%, 당일 시작 자산 대비). 초과 시 당일 신규 매수 중단 */
    private BigDecimal dailyLossLimitPct;

    /** VaR 계산 방법론 (PARAMETRIC | HISTORICAL) */
    private String varMethod;
    /** 역사적 VaR 룩백 기간 (거래일). HISTORICAL 방법 선택 시 사용 */
    private Integer varLookbackDays;
    /** 연간 손실 한도 비율 (%, 연초 자산 대비) */
    private BigDecimal yearEndLossLimitPct;
    /** 연간 손실 한도 알림 임계값 (0~1) */
    private BigDecimal yearEndAlertThresholdPct;
}
