package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 전략 거버넌스 halt (TB_GOVERNANCE_HALT).
 * (market, strategy_type)별 자동 매매 중단. CLEARED_AT IS NULL 이면 활성 halt.
 */
@Entity
@Table(name = "TB_GOVERNANCE_HALT")
@IdClass(GovernanceHaltId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GovernanceHalt {

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Id
    @Column(name = "STRATEGY_TYPE", nullable = false, length = 32)
    private String strategyType;

    @Column(name = "HALTED_AT", nullable = false)
    private Instant haltedAt;

    @Column(name = "REASON", length = 500)
    private String reason;

    @Column(name = "CLEARED_AT")
    private Instant clearedAt;

    @Column(name = "CLEARED_BY", length = 64)
    private String clearedBy;

    public static GovernanceHalt create(String market, String strategyType, String reason) {
        GovernanceHalt h = new GovernanceHalt();
        h.market = market;
        h.strategyType = strategyType;
        h.haltedAt = Instant.now();
        h.reason = reason;
        h.clearedAt = null;
        h.clearedBy = null;
        return h;
    }

    public boolean isActive() {
        return clearedAt == null;
    }

    public void clear(String clearedBy) {
        this.clearedAt = Instant.now();
        this.clearedBy = clearedBy;
    }

    /**
     * Clear된 halt를 다시 활성화 (열화 재발 시).
     */
    public void reactivate(String reason) {
        this.clearedAt = null;
        this.clearedBy = null;
        this.haltedAt = Instant.now();
        this.reason = reason;
    }
}
