package com.investment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 온보딩 퀴즈 결과: 프로필 및 전략 비율.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnboardingProfileResponseDto {

    /** 보수적 / 균형 / 공격적 */
    private String profile;

    private BigDecimal shortTermRatio;
    private BigDecimal mediumTermRatio;
    private BigDecimal longTermRatio;

    /** 설정에 적용했으면 true */
    private Boolean appliedToSettings;
}
