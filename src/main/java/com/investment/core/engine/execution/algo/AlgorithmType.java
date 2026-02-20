package com.investment.core.engine.execution.algo;

/**
 * 알고리즘 주문 유형.
 */
public enum AlgorithmType {

    /**
     * Time-Weighted Average Price.
     * 시간 균등 분할: 전체 수량을 N개 슬라이스로 나눠 일정 시간 간격으로 집행.
     */
    TWAP,

    /**
     * Volume-Weighted Average Price.
     * 거래량 가중 분할: 과거 거래량 프로필에 따라 거래량이 많은 시간대에 더 많이 집행.
     */
    VWAP,

    /**
     * Percentage of Volume.
     * 시장 참여율 기반: 실시간 거래량의 일정 비율(예: 10%)만큼만 집행.
     */
    POV
}
