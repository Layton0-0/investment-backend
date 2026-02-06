package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 계좌별 리스크 요약 (일일 손실 한도·MDD 등).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAccountSummaryDto {

    /** 계좌번호 마스킹 (예: ****1234) */
    private String accountNoMasked;
    /** 서버 타입 ("1": 모의, "0": 실거래) */
    private String serverType;
    /** 당일 시초 평가액 (기록된 경우만) */
    private BigDecimal openingBalance;
    /** 현재 평가액 */
    private BigDecimal currentValue;
    /** 일일 손실 한도 초과로 신규 매수 불가 여부 */
    private boolean newBuyBlockedByDailyLoss;
    /** MDD (0~1, 피크 대비 최대 낙폭). 없으면 null */
    private BigDecimal mdd;
    /** 피크 평가액 (MDD 계산 기준). 없으면 null */
    private BigDecimal peakValue;
}
