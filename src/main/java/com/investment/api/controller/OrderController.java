package com.investment.api.controller;

import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 주문 REST API
 */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestBody @Valid OrderRequestDto request) {
        OrderResponseDto response = orderService.executeOrder(request);
        return ResponseEntity.ok(response);
    }
    
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
}
