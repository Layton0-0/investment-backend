package com.investment.strategy.domain;

/**
 * 전략 상태
 */
public enum StrategyStatus {
    ACTIVE,   // 활성 (실행 중)
    STOPPED,  // 중지됨 (수동 시작 전까지 실행 안됨)
    PAUSED    // 일시 정지 (일시적으로 중지)
}
