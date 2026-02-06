package com.investment.core.engine.execution;

import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;

/**
 * 주문 집행 게이트웨이 — 한투 국내/해외 라우팅 추상화.
 * Phase 1: OrderService에 위임. Phase 2에서 Smart Order Router 확장 가능.
 */
public interface ExecutionGateway {

    /**
     * 주문 실행 (컴플라이언스 통과 후 호출).
     *
     * @param request 주문 요청
     * @param userId  사용자 ID
     * @return 주문 응답
     */
    OrderResponseDto execute(OrderRequestDto request, String userId);
}
