package com.investment.core.engine.execution;

import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;
import com.investment.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OrderService 기반 집행 게이트웨이.
 */
@Component
@RequiredArgsConstructor
public class OrderServiceExecutionGateway implements ExecutionGateway {

    private final OrderService orderService;

    @Override
    public OrderResponseDto execute(OrderRequestDto request, String userId) {
        return orderService.executeOrderForPipeline(request, userId);
    }
}
