package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 온보딩 퀴즈 결과: 프로필 및 단기/중기/장기 전략 비율.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingProfileResponseDto {

    /** 프로필: CONSERVATIVE, BALANCED, AGGRESSIVE */
    private String profile;

    private BigDecimal shortTermRatio;
    private BigDecimal mediumTermRatio;
    private BigDecimal longTermRatio;

    /** applyToSettings true였을 때 설정 반영 여부 */
    private Boolean appliedToSettings;
}
