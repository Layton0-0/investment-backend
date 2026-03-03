package com.investment.order.service;

import com.investment.order.dto.OrderRequestDto;
import com.investment.order.dto.OrderResponseDto;

/**
 * 파이프라인 주문 실행 추상화.
 * 단일 주문 즉시 실행(기본) 또는 알고리즘 실행(TWAP/VWAP 등)으로 확장 가능.
 *
 * @see OrderService#executeOrderForPipeline(OrderRequestDto, String)
 */
public interface PipelineOrderExecutor {

    /**
     * 파이프라인/청산용 주문 실행.
     *
     * @param request 주문 요청
     * @param userId  사용자 ID (토큰·계좌 매핑)
     * @return 주문 결과
     */
    OrderResponseDto executeOrderForPipeline(OrderRequestDto request, String userId);
}
