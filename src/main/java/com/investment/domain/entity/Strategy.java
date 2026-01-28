package com.investment.domain.entity;

import com.investment.strategy.domain.StrategyStatus;
import com.investment.strategy.domain.StrategyType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 투자 전략 엔티티
 */
@Entity
@Table(name = "TB_STRATEGIES", indexes = {
        @Index(name = "IDX_TB_STRATEGIES_USER_ID", columnList = "USER_ID"),
        @Index(name = "UK_TB_STRATEGIES_USER_ACCOUNT_TYPE", columnList = "USER_ID,ACCOUNT_NO,STRATEGY_TYPE", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Strategy {

    @Id
    @Column(name = "TB_STRATEGIES_UID", length = 36, nullable = false, unique = true)
    private String id;

    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;

    /**
     * 사용자 ID (선택적, 점진적 마이그레이션용)
     * 하위 호환성을 위해 NULL 허용
     */
    @Column(name = "USER_ID", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STRATEGY_TYPE", nullable = false, length = 20)
    private StrategyType strategyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private StrategyStatus status;

    @Column(name = "MAX_INVESTMENT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal maxInvestmentAmount;

    @Column(name = "MIN_INVESTMENT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal minInvestmentAmount;

    @Column(name = "RISK_LEVEL", precision = 3, scale = 2)
    private BigDecimal riskLevel;

    @Column(name = "CONFIDENCE_THRESHOLD", precision = 3, scale = 2)
    private BigDecimal confidenceThreshold; // 신뢰도 임계값

    @Column(name = "LAST_EXECUTED_AT")
    private LocalDateTime lastExecutedAt;

    @Column(name = "TOTAL_EXECUTIONS")
    private Long totalExecutions;

    @Column(name = "SUCCESS_COUNT")
    private Long successCount;

    @Column(name = "FAILURE_COUNT")
    private Long failureCount;

    @Column(name = "TOTAL_PROFIT_LOSS", precision = 18, scale = 2)
    private BigDecimal totalProfitLoss;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalExecutions == null) {
            totalExecutions = 0L;
        }
        if (successCount == null) {
            successCount = 0L;
        }
        if (failureCount == null) {
            failureCount = 0L;
        }
        if (totalProfitLoss == null) {
            totalProfitLoss = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Strategy(String accountNo, String userId, StrategyType strategyType, StrategyStatus status,
            BigDecimal maxInvestmentAmount, BigDecimal minInvestmentAmount,
            BigDecimal riskLevel, BigDecimal confidenceThreshold) {
        this.accountNo = accountNo;
        this.userId = userId;
        this.strategyType = strategyType;
        this.status = status != null ? status : StrategyStatus.ACTIVE;
        this.maxInvestmentAmount = maxInvestmentAmount;
        this.minInvestmentAmount = minInvestmentAmount;
        this.riskLevel = riskLevel;
        this.confidenceThreshold = confidenceThreshold;
    }

    /**
     * 사용자 ID 설정
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void activate() {
        this.status = StrategyStatus.ACTIVE;
    }

    public void stop() {
        this.status = StrategyStatus.STOPPED;
    }

    public void pause() {
        this.status = StrategyStatus.PAUSED;
    }

    public void updateMaxInvestmentAmount(BigDecimal maxInvestmentAmount) {
        this.maxInvestmentAmount = maxInvestmentAmount;
    }

    public void updateMinInvestmentAmount(BigDecimal minInvestmentAmount) {
        this.minInvestmentAmount = minInvestmentAmount;
    }

    public void updateRiskLevel(BigDecimal riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void updateConfidenceThreshold(BigDecimal confidenceThreshold) {
        this.confidenceThreshold = confidenceThreshold;
    }

    public void recordExecution(boolean success, BigDecimal profitLoss) {
        this.lastExecutedAt = LocalDateTime.now();
        this.totalExecutions++;
        if (success) {
            this.successCount++;
        } else {
            this.failureCount++;
        }
        if (profitLoss != null) {
            this.totalProfitLoss = this.totalProfitLoss.add(profitLoss);
        }
    }

    public boolean isActive() {
        return this.status == StrategyStatus.ACTIVE;
    }

    public boolean isStopped() {
        return this.status == StrategyStatus.STOPPED;
    }
}
