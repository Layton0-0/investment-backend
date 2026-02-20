package com.investment.risk.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Monte Carlo VaR/CVaR 계산 결과.
 */
@Getter
@Builder
public class MonteCarloVarResult {

    /** VaR 또는 CVaR 값 (%) */
    private final BigDecimal value;

    /** 신뢰수준 (예: 0.95) */
    private final BigDecimal confidenceLevel;

    /** 사용된 시나리오 수 */
    private final int scenarios;

    /** 계산 방법 (MONTE_CARLO_NORMAL, MONTE_CARLO_STUDENT_T) */
    private final String method;

    /** 사용된 분포의 자유도 (Student-t 분포인 경우) */
    private final Integer degreesOfFreedom;

    /** 계산에 사용된 샘플 수 */
    private final int sampleCount;

    /** 계산 소요 시간 (ms) */
    private final long elapsedTimeMs;

    /** 계산 시점 */
    private final Instant calculatedAt;

    /** 입력 데이터의 평균 수익률 */
    private final BigDecimal meanReturn;

    /** 입력 데이터의 표준편차 */
    private final BigDecimal stdDeviation;

    /** 시뮬레이션의 최대 손실값 */
    private final BigDecimal maxLoss;

    /** 시뮬레이션의 최소 손실값 */
    private final BigDecimal minLoss;

    /** 결과 유효 여부 */
    private final boolean valid;

    /** 에러 메시지 (valid=false 시) */
    private final String errorMessage;

    public static MonteCarloVarResult error(String message) {
        return MonteCarloVarResult.builder()
                .valid(false)
                .errorMessage(message)
                .calculatedAt(Instant.now())
                .build();
    }

    public static MonteCarloVarResult insufficientData(int sampleCount) {
        return MonteCarloVarResult.builder()
                .valid(false)
                .sampleCount(sampleCount)
                .errorMessage("Insufficient data: at least 60 samples required, got " + sampleCount)
                .calculatedAt(Instant.now())
                .build();
    }
}
