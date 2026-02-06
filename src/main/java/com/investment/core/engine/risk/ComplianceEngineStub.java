package com.investment.core.engine.risk;

import com.investment.order.dto.OrderRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Phase 1: 컴플라이언스 스텁 — 항상 승인.
 * investment.compliance.use-stub=true 일 때만 사용. 기본은 PreTradeComplianceEngine.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "investment.compliance.use-stub", havingValue = "true")
public class ComplianceEngineStub implements ComplianceEngine {

    @Override
    public ComplianceResult preTradeCheck(OrderRequestDto request, String userId) {
        return ComplianceResult.approve();
    }
}
