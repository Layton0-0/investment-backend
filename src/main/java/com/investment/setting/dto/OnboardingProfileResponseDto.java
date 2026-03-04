package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 온보딩 프로필 응답 (전략 비율 + 설정 반영 여부).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProfileResponseDto {

    /** CONSERVATIVE | BALANCED | AGGRESSIVE */
    private String profile;

    private BigDecimal shortTermRatio;
    private BigDecimal mediumTermRatio;
    private BigDecimal longTermRatio;

    /** applyToSettings true였을 때 실제 반영 여부 */
    private Boolean appliedToSettings;
}
