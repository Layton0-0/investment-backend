package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PUT /api/v1/ops/governance/halts/{market}/{strategyType}/clear 요청 body (선택).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GovernanceHaltClearRequestDto {

    @Size(max = 64)
    private String clearedBy;
}
