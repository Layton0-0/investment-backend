package com.investment.strategy.dto;

import com.investment.strategy.domain.StrategyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

/**
 * 전략 상태 업데이트 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyStatusUpdateDto {
    
    @NotNull(message = "상태는 필수입니다")
    private StrategyStatus status;
}
