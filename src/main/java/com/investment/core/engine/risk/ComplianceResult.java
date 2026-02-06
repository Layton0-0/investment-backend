package com.investment.core.engine.risk;

import lombok.Builder;
import lombok.Getter;

/**
 * 주문 직전 컴플라이언스 검사 결과.
 */
@Getter
@Builder
public class ComplianceResult {

    private final boolean approved;
    private final String reason;

    public static ComplianceResult approve() {
        return ComplianceResult.builder().approved(true).reason(null).build();
    }

    public static ComplianceResult reject(String reason) {
        return ComplianceResult.builder().approved(false).reason(reason).build();
    }
}
