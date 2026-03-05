package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 초보자 온보딩 퀴즈 요청 (3문항).
 * 투자기간·위험감수·투자금액 → 프로필(보수/균형/공격) 및 전략 비율 산출.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingQuizRequestDto {

    /** 투자 기간: 3M, 1Y, 3Y_PLUS */
    @NotBlank(message = "투자 기간을 선택해 주세요")
    private String investmentHorizon;

    /** 위험 감수: N5, N10, N20 */
    @NotBlank(message = "위험 감수를 선택해 주세요")
    private String riskTolerance;

    /** 투자 금액 구간: 1M, 5M, 10M_PLUS */
    @NotBlank(message = "투자 금액을 선택해 주세요")
    private String investmentAmount;

    /** true면 산출된 비율을 TradingSetting에 반영 */
    private Boolean applyToSettings;
}
