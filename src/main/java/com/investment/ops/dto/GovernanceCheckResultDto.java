package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 전략 거버넌스 검사 결과 한 건 (GET /api/v1/ops/governance/results 응답 항목).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GovernanceCheckResultDto {

    private Long id;
    private String runAt;
    private String market;
    private String strategyType;
    private BigDecimal mddPct;
    private BigDecimal sharpeRatio;
    private boolean degraded;
    private LocalDate startDate;
    private LocalDate endDate;
    private String createdAt;
}
