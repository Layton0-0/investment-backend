package com.investment.backtest.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Walk-Forward(롤링 Out-of-Sample) 백테스트 요청.
 * <p>구간을 train/test 윈도우로 나누어 각 test 구간만 백테스트 실행 후 메트릭을 집계하여
 * 오버피팅 완화·일반화 성능 추정에 사용한다. 전략 파라미터는 기존 설정을 그대로 사용한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkForwardBacktestRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private String market;

    @NotNull
    private String strategyType;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax(value = "1000000000000")
    private BigDecimal initialCapital;

    /** Train 윈도우 일수 (파라미터 추정 구간, 현재 구현에서는 test만 실행). 기본 252(연 1년). */
    @Builder.Default
    private int trainDays = 252;

    /** Test(OOS) 윈도우 일수. 기본 63(분기). */
    @Builder.Default
    private int testDays = 63;

    /** 다음 fold로 진행 시 이동 일수. stepDays == testDays 이면 비중첩. 기본 63. */
    @Builder.Default
    private int stepDays = 63;
}
