package com.investment.core.engine.execution.algo;

import java.util.List;

/**
 * 주문 분할 실행 알고리즘 인터페이스.
 */
public interface ExecutionAlgorithm {

    /**
     * 알고리즘 유형 반환.
     */
    AlgorithmType getType();

    /**
     * 주문을 슬라이스로 분할.
     *
     * @param order 알고리즘 주문
     * @return 슬라이스 계획
     */
    List<SlicePlan> planSlices(AlgorithmicOrder order);

    /**
     * 슬라이스 계획 (분할 주문 일정).
     */
    record SlicePlan(
            int sliceNumber,
            int quantity,
            long delayMillis,
            double targetVolumeRatio
    ) {}
}
