package com.investment.setting.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 온보딩 퀴즈 요청 (3문항: 투자기간·위험감수·투자금액).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingQuizRequestDto {

    /** 3M | 1Y | 3Y_PLUS */
    @NotBlank(message = "투자 기간을 선택해 주세요")
    private String investmentHorizon;

    /** N5 | N10 | N20 */
    @NotBlank(message = "손실 감수를 선택해 주세요")
    private String riskTolerance;

    /** 1M | 5M | 10M_PLUS */
    @NotBlank(message = "투자 예정 금액을 선택해 주세요")
    private String investmentAmount;

    /** true 시 인증 사용자의 TradingSetting에 비율 반영 */
    private Boolean applyToSettings;

    /** 미설정 시 사용자 첫 계좌에 적용 */
    private String accountNo;
}
