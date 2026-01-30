package com.investment.strategy.dto;

import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 전략 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StrategyDto {
    
    private String strategyId;
    
    @NotNull(message = "계좌번호는 필수입니다")
    private String accountNo;

    /** 시장 (KR, US). 미지정 시 KR */
    private String market;

    @NotNull(message = "전략 타입은 필수입니다")
    private StrategyType strategyType;
    
    @NotNull(message = "상태는 필수입니다")
    private StrategyStatus status;
    
    private BigDecimal maxInvestmentAmount;
    
    private BigDecimal minInvestmentAmount;
    
    private BigDecimal riskLevel;
    
    private BigDecimal confidenceThreshold;
    
    private LocalDateTime lastExecutedAt;
    
    private Long totalExecutions;
    
    private Long successCount;
    
    private Long failureCount;
    
    private BigDecimal totalProfitLoss;
    
    private BigDecimal successRate; // 성공률
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
