package com.investment.api.controller;

import com.investment.core.engine.execution.algo.*;
import com.investment.core.engine.execution.algo.AlgorithmicOrder.AlgorithmParameters;
import com.investment.core.engine.execution.algo.AlgorithmicOrder.OrderSide;
import com.investment.core.engine.execution.algo.ExecutionAlgorithm.SlicePlan;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 알고리즘 주문 REST API.
 */
@RestController
@RequestMapping("/api/v1/algo-orders")
@RequiredArgsConstructor
@Tag(name = "AlgorithmicOrder", description = "TWAP/VWAP/POV 알고리즘 주문 API")
public class AlgorithmicOrderController {

    private final AlgorithmicOrderService algorithmicOrderService;

    @PostMapping("/execute")
    @Operation(summary = "알고리즘 주문 실행", description = "TWAP/VWAP/POV 알고리즘으로 주문을 분할 실행합니다.")
    public ResponseEntity<AlgorithmicExecutionResult> executeOrder(
            @RequestBody AlgorithmicOrderRequest request,
            Authentication authentication) {

        String userId = authentication != null ? authentication.getName() : "anonymous";

        AlgorithmicOrder order = AlgorithmicOrder.builder()
                .orderId(request.orderId())
                .symbol(request.symbol())
                .market(request.market() != null ? request.market() : "KR")
                .side(request.side())
                .totalQuantity(request.totalQuantity())
                .limitPrice(request.limitPrice())
                .algorithm(request.algorithm())
                .parameters(request.parameters() != null
                        ? request.parameters()
                        : getDefaultParameters(request.algorithm()))
                .build();

        AlgorithmicExecutionResult result = algorithmicOrderService.execute(order, userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{executionId}")
    @Operation(summary = "실행 진행 상태 조회", description = "알고리즘 주문의 현재 진행 상태를 조회합니다.")
    public ResponseEntity<AlgorithmicExecutionResult> getProgress(
            @Parameter(description = "실행 ID") @PathVariable String executionId) {

        AlgorithmicExecutionResult result = algorithmicOrderService.getProgress(executionId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{executionId}/cancel")
    @Operation(summary = "실행 취소", description = "진행 중인 알고리즘 주문을 취소합니다.")
    public ResponseEntity<AlgorithmicExecutionResult> cancelOrder(
            @Parameter(description = "실행 ID") @PathVariable String executionId) {

        AlgorithmicExecutionResult result = algorithmicOrderService.cancel(executionId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{executionId}/resume")
    @Operation(summary = "실행 재개", description = "취소된 알고리즘 주문을 재개합니다.")
    public ResponseEntity<AlgorithmicExecutionResult> resumeOrder(
            @Parameter(description = "실행 ID") @PathVariable String executionId) {

        AlgorithmicExecutionResult result = algorithmicOrderService.resume(executionId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/active")
    @Operation(summary = "활성 주문 목록", description = "현재 진행 중인 알고리즘 주문 목록을 조회합니다.")
    public ResponseEntity<List<AlgorithmicExecutionResult>> getActiveOrders(
            Authentication authentication) {

        String userId = authentication != null ? authentication.getName() : "anonymous";
        List<AlgorithmicExecutionResult> results = algorithmicOrderService.getActiveExecutions(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/algorithms")
    @Operation(summary = "지원 알고리즘 목록", description = "사용 가능한 알고리즘 유형 목록을 조회합니다.")
    public ResponseEntity<List<AlgorithmType>> getSupportedAlgorithms() {
        return ResponseEntity.ok(algorithmicOrderService.getSupportedAlgorithms());
    }

    @PostMapping("/preview")
    @Operation(summary = "슬라이스 계획 미리보기", description = "주문 분할 계획을 미리 확인합니다.")
    public ResponseEntity<List<SlicePlan>> previewSlices(
            @RequestBody AlgorithmicOrderRequest request) {

        AlgorithmicOrder order = AlgorithmicOrder.builder()
                .orderId(request.orderId())
                .symbol(request.symbol())
                .market(request.market() != null ? request.market() : "KR")
                .side(request.side())
                .totalQuantity(request.totalQuantity())
                .limitPrice(request.limitPrice())
                .algorithm(request.algorithm())
                .parameters(request.parameters() != null
                        ? request.parameters()
                        : getDefaultParameters(request.algorithm()))
                .build();

        List<SlicePlan> slices = algorithmicOrderService.previewSlices(order);
        return ResponseEntity.ok(slices);
    }

    private AlgorithmParameters getDefaultParameters(AlgorithmType algorithm) {
        return switch (algorithm) {
            case TWAP -> AlgorithmParameters.defaultTwap();
            case VWAP -> AlgorithmParameters.defaultVwap();
            case POV -> AlgorithmParameters.defaultPov();
        };
    }

    public record AlgorithmicOrderRequest(
            String orderId,
            String symbol,
            String market,
            OrderSide side,
            int totalQuantity,
            BigDecimal limitPrice,
            AlgorithmType algorithm,
            AlgorithmParameters parameters
    ) {}
}
