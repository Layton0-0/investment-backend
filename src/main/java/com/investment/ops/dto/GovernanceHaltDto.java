package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전략 거버넌스 halt 한 건 (GET /api/v1/ops/governance/halts 응답 항목).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GovernanceHaltDto {

    private String market;
    private String strategyType;
    private String haltedAt;
    private String reason;
}
