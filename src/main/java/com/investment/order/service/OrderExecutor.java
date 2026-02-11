package com.investment.order.service;

import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;

/**
 * 주문 실행을 수행하는 인터페이스.
 * OrderRequestQueue의 단일 소비자 스레드가 이 인터페이스를 통해 실제 주문을 실행합니다.
 */
@FunctionalInterface
public interface OrderExecutor {

    /**
     * 주문을 실행합니다.
     *
     * @param request 주문 요청
     * @param userId  사용자 ID (한국투자증권 API 토큰·계좌 매핑용)
     * @return 주문 응답
     */
    OrderResponseDto execute(OrderRequestDto request, String userId);
}
