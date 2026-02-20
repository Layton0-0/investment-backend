package com.investment.core.engine.execution.algo;

import java.util.List;

/**
 * 알고리즘 주문 서비스 인터페이스.
 */
public interface AlgorithmicOrderService {

    /**
     * 알고리즘 주문 실행 시작.
     *
     * @param order 알고리즘 주문
     * @param userId 사용자 ID
     * @return 실행 결과 (초기 상태)
     */
    AlgorithmicExecutionResult execute(AlgorithmicOrder order, String userId);

    /**
     * 실행 진행 상태 조회.
     *
     * @param executionId 실행 ID
     * @return 실행 결과 (현재 상태)
     */
    AlgorithmicExecutionResult getProgress(String executionId);

    /**
     * 실행 취소.
     *
     * @param executionId 실행 ID
     * @return 취소 후 결과
     */
    AlgorithmicExecutionResult cancel(String executionId);

    /**
     * 실행 재개.
     *
     * @param executionId 실행 ID
     * @return 재개 후 결과
     */
    AlgorithmicExecutionResult resume(String executionId);

    /**
     * 사용자의 활성 알고리즘 주문 목록 조회.
     *
     * @param userId 사용자 ID
     * @return 활성 실행 목록
     */
    List<AlgorithmicExecutionResult> getActiveExecutions(String userId);

    /**
     * 지원하는 알고리즘 목록 조회.
     *
     * @return 알고리즘 유형 목록
     */
    List<AlgorithmType> getSupportedAlgorithms();

    /**
     * 슬라이스 계획 미리보기.
     *
     * @param order 알고리즘 주문
     * @return 슬라이스 계획
     */
    List<ExecutionAlgorithm.SlicePlan> previewSlices(AlgorithmicOrder order);
}
