package com.investment.core.engine.execution.algo;

import com.investment.core.engine.execution.ExecutionGateway;
import com.investment.core.engine.execution.algo.AlgorithmicExecutionResult.ExecutionStatus;
import com.investment.core.engine.execution.algo.AlgorithmicExecutionResult.SliceExecution;
import com.investment.core.engine.execution.algo.AlgorithmicExecutionResult.SliceExecution.SliceStatus;
import com.investment.core.engine.execution.algo.AlgorithmicOrder.OrderSide;
import com.investment.core.engine.execution.algo.ExecutionAlgorithm.SlicePlan;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 알고리즘 주문 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmicOrderServiceImpl implements AlgorithmicOrderService {

    private final ExecutionGateway executionGateway;
    private final TaskScheduler taskScheduler;

    private final Map<AlgorithmType, ExecutionAlgorithm> algorithms;

    private final Map<String, AlgorithmicExecutionResult> executions = new ConcurrentHashMap<>();
    private final Map<String, String> executionToUser = new ConcurrentHashMap<>();
    private final Map<String, List<SliceExecution>> sliceExecutions = new ConcurrentHashMap<>();

    public AlgorithmicOrderServiceImpl(
            ExecutionGateway executionGateway,
            TaskScheduler taskScheduler,
            TwapExecutionAlgorithm twapAlgorithm,
            VwapExecutionAlgorithm vwapAlgorithm,
            PovExecutionAlgorithm povAlgorithm) {

        this.executionGateway = executionGateway;
        this.taskScheduler = taskScheduler;

        this.algorithms = Map.of(
                AlgorithmType.TWAP, twapAlgorithm,
                AlgorithmType.VWAP, vwapAlgorithm,
                AlgorithmType.POV, povAlgorithm
        );
    }

    @Override
    public AlgorithmicExecutionResult execute(AlgorithmicOrder order, String userId) {
        String executionId = UUID.randomUUID().toString();

        ExecutionAlgorithm algorithm = algorithms.get(order.getAlgorithm());
        if (algorithm == null) {
            return AlgorithmicExecutionResult.failed(executionId, order,
                    "Unsupported algorithm: " + order.getAlgorithm());
        }

        AlgorithmicExecutionResult result = AlgorithmicExecutionResult.pending(executionId, order);
        executions.put(executionId, result);
        executionToUser.put(executionId, userId);

        List<SlicePlan> plans = algorithm.planSlices(order);
        List<SliceExecution> slices = plans.stream()
                .map(plan -> SliceExecution.builder()
                        .sliceNumber(plan.sliceNumber())
                        .targetQuantity(plan.quantity())
                        .filledQuantity(0)
                        .status(SliceStatus.PENDING)
                        .scheduledAt(Instant.now().plusMillis(plan.delayMillis()))
                        .build())
                .collect(Collectors.toList());

        sliceExecutions.put(executionId, slices);

        updateExecutionStatus(executionId, ExecutionStatus.IN_PROGRESS);

        scheduleSlices(executionId, order, plans, userId);

        log.info("Algorithmic order started: executionId={}, algorithm={}, symbol={}, qty={}",
                executionId, order.getAlgorithm(), order.getSymbol(), order.getTotalQuantity());

        return executions.get(executionId);
    }

    @Override
    public AlgorithmicExecutionResult getProgress(String executionId) {
        AlgorithmicExecutionResult result = executions.get(executionId);
        if (result == null) {
            return null;
        }

        List<SliceExecution> slices = sliceExecutions.get(executionId);
        return AlgorithmicExecutionResult.builder()
                .executionId(result.getExecutionId())
                .orderId(result.getOrderId())
                .symbol(result.getSymbol())
                .market(result.getMarket())
                .algorithm(result.getAlgorithm())
                .status(result.getStatus())
                .totalQuantity(result.getTotalQuantity())
                .filledQuantity(result.getFilledQuantity())
                .remainingQuantity(result.getRemainingQuantity())
                .avgFilledPrice(result.getAvgFilledPrice())
                .targetPrice(result.getTargetPrice())
                .slippagePct(result.getSlippagePct())
                .marketVwap(result.getMarketVwap())
                .progressPct(result.getProgressPct())
                .slices(slices)
                .startedAt(result.getStartedAt())
                .completedAt(result.getCompletedAt())
                .errorMessage(result.getErrorMessage())
                .build();
    }

    @Override
    public AlgorithmicExecutionResult cancel(String executionId) {
        AlgorithmicExecutionResult result = executions.get(executionId);
        if (result == null) {
            return null;
        }

        if (result.getStatus() == ExecutionStatus.COMPLETED ||
                result.getStatus() == ExecutionStatus.CANCELLED) {
            return result;
        }

        updateExecutionStatus(executionId, ExecutionStatus.CANCELLED);

        List<SliceExecution> slices = sliceExecutions.get(executionId);
        if (slices != null) {
            for (int i = 0; i < slices.size(); i++) {
                SliceExecution slice = slices.get(i);
                if (slice.getStatus() == SliceStatus.PENDING) {
                    slices.set(i, SliceExecution.builder()
                            .sliceNumber(slice.getSliceNumber())
                            .targetQuantity(slice.getTargetQuantity())
                            .filledQuantity(slice.getFilledQuantity())
                            .avgPrice(slice.getAvgPrice())
                            .scheduledAt(slice.getScheduledAt())
                            .executedAt(slice.getExecutedAt())
                            .status(SliceStatus.CANCELLED)
                            .childOrderId(slice.getChildOrderId())
                            .build());
                }
            }
        }

        log.info("Algorithmic order cancelled: executionId={}", executionId);
        return executions.get(executionId);
    }

    @Override
    public AlgorithmicExecutionResult resume(String executionId) {
        AlgorithmicExecutionResult result = executions.get(executionId);
        if (result == null) {
            return null;
        }

        if (result.getStatus() != ExecutionStatus.CANCELLED &&
                result.getStatus() != ExecutionStatus.PARTIALLY_FILLED) {
            return result;
        }

        updateExecutionStatus(executionId, ExecutionStatus.IN_PROGRESS);
        log.info("Algorithmic order resumed: executionId={}", executionId);

        return executions.get(executionId);
    }

    @Override
    public List<AlgorithmicExecutionResult> getActiveExecutions(String userId) {
        return executions.entrySet().stream()
                .filter(e -> userId.equals(executionToUser.get(e.getKey())))
                .filter(e -> e.getValue().getStatus() == ExecutionStatus.IN_PROGRESS ||
                        e.getValue().getStatus() == ExecutionStatus.PARTIALLY_FILLED)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public List<AlgorithmType> getSupportedAlgorithms() {
        return Arrays.asList(AlgorithmType.values());
    }

    @Override
    public List<SlicePlan> previewSlices(AlgorithmicOrder order) {
        ExecutionAlgorithm algorithm = algorithms.get(order.getAlgorithm());
        if (algorithm == null) {
            return Collections.emptyList();
        }
        return algorithm.planSlices(order);
    }

    private void scheduleSlices(String executionId, AlgorithmicOrder order,
                                 List<SlicePlan> plans, String userId) {
        for (SlicePlan plan : plans) {
            Instant scheduledTime = Instant.now().plusMillis(plan.delayMillis());

            taskScheduler.schedule(
                    () -> executeSlice(executionId, order, plan, userId),
                    scheduledTime
            );
        }
    }

    private void executeSlice(String executionId, AlgorithmicOrder order,
                               SlicePlan plan, String userId) {
        AlgorithmicExecutionResult current = executions.get(executionId);
        if (current == null ||
                current.getStatus() == ExecutionStatus.CANCELLED ||
                current.getStatus() == ExecutionStatus.COMPLETED) {
            return;
        }

        try {
            OrderRequestDto orderRequest = OrderRequestDto.builder()
                    .symbol(order.getSymbol())
                    .market(order.getMarket())
                    .orderType(order.getSide() == OrderSide.BUY
                            ? OrderRequestDto.OrderType.BUY
                            : OrderRequestDto.OrderType.SELL)
                    .quantity(plan.quantity())
                    .price(order.getLimitPrice())
                    .build();

            OrderResponseDto response = executionGateway.execute(orderRequest, userId);

            updateSliceExecution(executionId, plan.sliceNumber(), response);
            updateOverallProgress(executionId, order);

            log.debug("Slice executed: executionId={}, slice={}, qty={}, response={}",
                    executionId, plan.sliceNumber(), plan.quantity(),
                    response != null ? response.getOrderId() : "null");

        } catch (Exception e) {
            log.error("Slice execution failed: executionId={}, slice={}",
                    executionId, plan.sliceNumber(), e);

            markSliceFailed(executionId, plan.sliceNumber());
        }
    }

    private void updateSliceExecution(String executionId, int sliceNumber,
                                       OrderResponseDto response) {
        List<SliceExecution> slices = sliceExecutions.get(executionId);
        if (slices == null) return;

        for (int i = 0; i < slices.size(); i++) {
            SliceExecution slice = slices.get(i);
            if (slice.getSliceNumber() == sliceNumber) {
                boolean isSuccess = response != null &&
                            response.getStatus() == OrderResponseDto.OrderStatus.EXECUTED;
                SliceStatus status = isSuccess ? SliceStatus.FILLED : SliceStatus.FAILED;

                slices.set(i, SliceExecution.builder()
                        .sliceNumber(slice.getSliceNumber())
                        .targetQuantity(slice.getTargetQuantity())
                        .filledQuantity(isSuccess ? slice.getTargetQuantity() : 0)
                        .avgPrice(response != null ? response.getPrice() : null)
                        .scheduledAt(slice.getScheduledAt())
                        .executedAt(Instant.now())
                        .status(status)
                        .childOrderId(response != null ? response.getOrderId() : null)
                        .build());
                break;
            }
        }
    }

    private void markSliceFailed(String executionId, int sliceNumber) {
        List<SliceExecution> slices = sliceExecutions.get(executionId);
        if (slices == null) return;

        for (int i = 0; i < slices.size(); i++) {
            SliceExecution slice = slices.get(i);
            if (slice.getSliceNumber() == sliceNumber) {
                slices.set(i, SliceExecution.builder()
                        .sliceNumber(slice.getSliceNumber())
                        .targetQuantity(slice.getTargetQuantity())
                        .filledQuantity(0)
                        .avgPrice(null)
                        .scheduledAt(slice.getScheduledAt())
                        .executedAt(Instant.now())
                        .status(SliceStatus.FAILED)
                        .childOrderId(null)
                        .build());
                break;
            }
        }
    }

    private void updateOverallProgress(String executionId, AlgorithmicOrder order) {
        List<SliceExecution> slices = sliceExecutions.get(executionId);
        if (slices == null) return;

        int filledQuantity = slices.stream()
                .mapToInt(SliceExecution::getFilledQuantity)
                .sum();

        BigDecimal avgPrice = calculateAvgFilledPrice(slices);

        long completedSlices = slices.stream()
                .filter(s -> s.getStatus() == SliceStatus.FILLED ||
                        s.getStatus() == SliceStatus.FAILED ||
                        s.getStatus() == SliceStatus.CANCELLED)
                .count();

        AlgorithmicExecutionResult current = executions.get(executionId);

        BigDecimal progressPct = BigDecimal.valueOf(completedSlices)
                .divide(BigDecimal.valueOf(slices.size()), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        ExecutionStatus status = current.getStatus();
        if (completedSlices == slices.size()) {
            status = filledQuantity == order.getTotalQuantity()
                    ? ExecutionStatus.COMPLETED
                    : ExecutionStatus.PARTIALLY_FILLED;
        }

        AlgorithmicExecutionResult updated = AlgorithmicExecutionResult.builder()
                .executionId(current.getExecutionId())
                .orderId(current.getOrderId())
                .symbol(current.getSymbol())
                .market(current.getMarket())
                .algorithm(current.getAlgorithm())
                .status(status)
                .totalQuantity(current.getTotalQuantity())
                .filledQuantity(filledQuantity)
                .remainingQuantity(current.getTotalQuantity() - filledQuantity)
                .avgFilledPrice(avgPrice)
                .targetPrice(current.getTargetPrice())
                .slippagePct(current.getSlippagePct())
                .marketVwap(current.getMarketVwap())
                .progressPct(progressPct)
                .slices(slices)
                .startedAt(current.getStartedAt())
                .completedAt(status == ExecutionStatus.COMPLETED ? Instant.now() : null)
                .errorMessage(current.getErrorMessage())
                .build();

        executions.put(executionId, updated);
    }

    private BigDecimal calculateAvgFilledPrice(List<SliceExecution> slices) {
        BigDecimal sumPriceQty = BigDecimal.ZERO;
        int sumQty = 0;

        for (SliceExecution slice : slices) {
            if (slice.getAvgPrice() != null && slice.getFilledQuantity() > 0) {
                sumPriceQty = sumPriceQty.add(
                        slice.getAvgPrice().multiply(BigDecimal.valueOf(slice.getFilledQuantity())));
                sumQty += slice.getFilledQuantity();
            }
        }

        if (sumQty == 0) {
            return null;
        }

        return sumPriceQty.divide(BigDecimal.valueOf(sumQty), 4, RoundingMode.HALF_UP);
    }

    private void updateExecutionStatus(String executionId, ExecutionStatus status) {
        AlgorithmicExecutionResult current = executions.get(executionId);
        if (current == null) return;

        AlgorithmicExecutionResult updated = AlgorithmicExecutionResult.builder()
                .executionId(current.getExecutionId())
                .orderId(current.getOrderId())
                .symbol(current.getSymbol())
                .market(current.getMarket())
                .algorithm(current.getAlgorithm())
                .status(status)
                .totalQuantity(current.getTotalQuantity())
                .filledQuantity(current.getFilledQuantity())
                .remainingQuantity(current.getRemainingQuantity())
                .avgFilledPrice(current.getAvgFilledPrice())
                .targetPrice(current.getTargetPrice())
                .slippagePct(current.getSlippagePct())
                .marketVwap(current.getMarketVwap())
                .progressPct(current.getProgressPct())
                .startedAt(current.getStartedAt())
                .completedAt(status == ExecutionStatus.COMPLETED ||
                        status == ExecutionStatus.CANCELLED ? Instant.now() : null)
                .errorMessage(current.getErrorMessage())
                .build();

        executions.put(executionId, updated);
    }
}
