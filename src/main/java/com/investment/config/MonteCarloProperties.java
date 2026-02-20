package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Monte Carlo 시뮬레이션 설정.
 */
@Component
@ConfigurationProperties(prefix = "investment.risk.montecarlo")
@Getter
@Setter
public class MonteCarloProperties {

    /** 기본 시뮬레이션 시나리오 수 (기본 10,000) */
    private int scenarios = 10000;

    /** 분포 유형 (NORMAL, STUDENT_T) */
    private Distribution distribution = Distribution.STUDENT_T;

    /** Student-t 분포 자유도 (기본 5, 팻테일 효과) */
    private int degreesOfFreedom = 5;

    /** 병렬 처리 스레드 수 (기본 4) */
    private int parallelThreads = 4;

    /** 최소 필요 샘플 수 (기본 60) */
    private int minSamples = 60;

    /** 기본 신뢰수준 (기본 0.95 = 95%) */
    private double defaultConfidenceLevel = 0.95;

    /** 비동기 처리 타임아웃 (초, 기본 60) */
    private int asyncTimeoutSeconds = 60;

    /** 랜덤 시드 고정 여부 (테스트용, 기본 false) */
    private boolean fixedSeed = false;

    /** 고정 시드 값 (fixedSeed=true 시 사용) */
    private long seedValue = 42L;

    public enum Distribution {
        /** 정규분포 */
        NORMAL,
        /** Student-t 분포 (팻테일) */
        STUDENT_T
    }
}
