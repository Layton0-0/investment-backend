package com.investment.order.service;

import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 파이프라인 주문을 단일 주문 즉시 실행으로 처리.
 * pipeline.use-algo-execution=true 시 TWAP/VWAP 등 알고리즘 실행으로 교체 가능.
 */
@Service
@RequiredArgsConstructor
public class DefaultPipelineOrderExecutor implements PipelineOrderExecutor {

    private final OrderService orderService;

    @Override
    public OrderResponseDto executeOrderForPipeline(OrderRequestDto request, String userId) {
        return orderService.executeOrderForPipeline(request, userId);
    }
}
