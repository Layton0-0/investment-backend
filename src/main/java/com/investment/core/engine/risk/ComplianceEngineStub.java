package com.investment.core.engine.risk;

import com.investment.order.dto.OrderRequestDto;
import lombok.extern.slf4j.Slf4j;

/**
 * 컴플라이언스 스텁 — 항상 승인. 테스트 전용.
 * 프로덕션에서는 PreTradeComplianceEngine이 디폴트. 테스트에서 스텁이 필요하면
 * @TestConfiguration에서 이 클래스를 Bean으로 노출하여 사용.
 */
@Slf4j
public class ComplianceEngineStub implements ComplianceEngine {

    @Override
    public ComplianceResult preTradeCheck(OrderRequestDto request, String userId) {
        return ComplianceResult.approve();
    }
}
