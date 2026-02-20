package com.investment.core.engine.execution.algo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * 알고리즘 주문 집행 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmicExecutionResult {

    private String executionId;
    private String orderId;
    private String symbol;
    private String market;
    private AlgorithmType algorithm;

    private ExecutionStatus status;
    private int totalQuantity;
    private int filledQuantity;
    private int remainingQuantity;

    /**
     * 평균 체결가.
     */
    private BigDecimal avgFilledPrice;

    /**
     * 목표 TWAP/VWAP 가격.
     */
    private BigDecimal targetPrice;

    /**
     * 실제 vs 목표 괴리율 (%).
     */
    private BigDecimal slippagePct;

    /**
     * 시장 VWAP (비교용).
     */
    private BigDecimal marketVwap;

    /**
     * 집행 진행률 (%).
     */
    private BigDecimal progressPct;

    /**
     * 슬라이스별 집행 내역.
     */
    private List<SliceExecution> slices;

    /**
     * 집행 시작 시간.
     */
    private Instant startedAt;

    /**
     * 집행 종료 시간.
     */
    private Instant completedAt;

    /**
     * 에러 메시지 (실패 시).
     */
    private String errorMessage;

    public enum ExecutionStatus {
        PENDING,
        IN_PROGRESS,
        PARTIALLY_FILLED,
        COMPLETED,
        CANCELLED,
        FAILED
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SliceExecution {
        private int sliceNumber;
        private int targetQuantity;
        private int filledQuantity;
        private BigDecimal avgPrice;
        private Instant scheduledAt;
        private Instant executedAt;
        private SliceStatus status;
        private String childOrderId;

        public enum SliceStatus {
            PENDING,
            SENT,
            FILLED,
            PARTIALLY_FILLED,
            CANCELLED,
            FAILED
        }
    }

    public static AlgorithmicExecutionResult pending(String executionId, AlgorithmicOrder order) {
        return AlgorithmicExecutionResult.builder()
                .executionId(executionId)
                .orderId(order.getOrderId())
                .symbol(order.getSymbol())
                .market(order.getMarket())
                .algorithm(order.getAlgorithm())
                .status(ExecutionStatus.PENDING)
                .totalQuantity(order.getTotalQuantity())
                .filledQuantity(0)
                .remainingQuantity(order.getTotalQuantity())
                .progressPct(BigDecimal.ZERO)
                .startedAt(Instant.now())
                .build();
    }

    public static AlgorithmicExecutionResult failed(String executionId, AlgorithmicOrder order, String errorMessage) {
        return AlgorithmicExecutionResult.builder()
                .executionId(executionId)
                .orderId(order.getOrderId())
                .symbol(order.getSymbol())
                .market(order.getMarket())
                .algorithm(order.getAlgorithm())
                .status(ExecutionStatus.FAILED)
                .totalQuantity(order.getTotalQuantity())
                .filledQuantity(0)
                .remainingQuantity(order.getTotalQuantity())
                .progressPct(BigDecimal.ZERO)
                .errorMessage(errorMessage)
                .build();
    }
}
