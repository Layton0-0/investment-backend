package com.investment.api.controller;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * 주문 REST API
 */
@Tag(name = "Order", description = "주문 관리 API")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @Operation(
            summary = "주문 실행",
            description = "주식 매수/매도 주문을 실행합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "주문 성공",
                    content = @Content(schema = @Schema(implementation = OrderResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (금액 제한 초과 등)"),
            @ApiResponse(responseCode = "404", description = "설정을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<?> createOrder(
            @RequestBody @Valid OrderRequestDto request) {
        try {
            OrderResponseDto response = orderService.executeOrder(request);
            return ResponseEntity.ok(response);
        } catch (DomainException e) {
            int status = ErrorCode.ORDER_REJECTED.equals(e.getErrorCode()) ? 403 : 400;
            return ResponseEntity.status(status).body(
                    new OrderErrorBody(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new OrderErrorBody(ErrorCode.ORDER_FAILED, "주문 처리 중 오류가 발생했습니다."));
        } catch (Throwable t) {
            return ResponseEntity.badRequest().body(
                    new OrderErrorBody(ErrorCode.ORDER_FAILED, "주문 처리 중 오류가 발생했습니다."));
        }
    }

    /** 주문 실패 시 응답 본문 (4xx). */
    public record OrderErrorBody(String code, String message) {}
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable String orderId,
            @RequestParam @NotBlank String accountNo) {
        OrderResponseDto response = orderService.getOrder(orderId, accountNo);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestParam @NotBlank String accountNo) {
        List<OrderResponseDto> orders = orderService.getOrders(accountNo);
        return ResponseEntity.ok(orders);
    }
    
    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable String orderId,
            @RequestParam @NotBlank String accountNo) {
        orderService.cancelOrder(orderId, accountNo);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "미체결 전체 취소", description = "해당 계좌의 대기 중인 주문을 모두 취소합니다.")
    @PostMapping("/cancel-all-pending")
    public ResponseEntity<CancelAllPendingResult> cancelAllPending(
            @RequestParam @NotBlank String accountNo) {
        int count = orderService.cancelAllPendingOrders(accountNo);
        return ResponseEntity.ok(new CancelAllPendingResult(accountNo, count));
    }

    /** 미체결 전체 취소 응답 (취소된 건수). */
    public static final class CancelAllPendingResult {
        private final String accountNo;
        private final int cancelledCount;

        public CancelAllPendingResult(String accountNo, int cancelledCount) {
            this.accountNo = accountNo;
            this.cancelledCount = cancelledCount;
        }

        public String getAccountNo() { return accountNo; }
        public int getCancelledCount() { return cancelledCount; }
    }
}
