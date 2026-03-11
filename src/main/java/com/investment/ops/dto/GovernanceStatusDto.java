package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전략 거버넌스 검사 활성 여부 (GET /api/v1/ops/governance/status 응답).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GovernanceStatusDto {

    /** 시스템 설정 governance.enabled — true면 검사 Job 실행 시 백테스트·결과 저장 수행 */
    private Boolean governanceEnabled;
}
