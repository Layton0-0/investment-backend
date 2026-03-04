package com.investment.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 초보자 온보딩 퀴즈 요청 (3문항).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingQuizRequestDto {

    /** 투자 기간: 3M, 1Y, 3Y_PLUS */
    @NotBlank
    private String investmentHorizon;

    /** 위험 감수: N5 (약 -5% 남을 때), N10, N20 */
    @NotBlank
    private String riskTolerance;

    /** 투자 금액: 1M, 5M, 10M_PLUS (만원 단위) */
    @NotBlank
    private String investmentAmount;

    /** true면 응답자의 TradingSetting에 전략 비율 자동 반영 (accountNo 필요) */
    private Boolean applyToSettings;

    /** applyToSettings 시 적용할 계좌번호 (미설정 시 해당 사용자 첫 계좌) */
    private String accountNo;
}
