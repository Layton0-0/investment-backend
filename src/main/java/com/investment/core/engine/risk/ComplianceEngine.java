package com.investment.core.engine.risk;

import com.investment.order.dto.OrderRequestDto;

/**
 * 주문 직전 컴플라이언스(Pre-Trade) 검사.
 * 개별 종목 비중 상한, MDD 게이트, Kill Switch 등 Phase 2에서 구현.
 */
public interface ComplianceEngine {

    /**
     * 주문 실행 전 검사.
     *
     * @param request 주문 요청
     * @param userId  사용자 ID (계좌·포지션 조회용)
     * @return 승인 시 approved=true, 거부 시 approved=false 및 reason
     */
    ComplianceResult preTradeCheck(OrderRequestDto request, String userId);
}
