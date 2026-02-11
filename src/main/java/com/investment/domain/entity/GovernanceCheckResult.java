package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * 전략 거버넌스 검사 결과 (TB_GOVERNANCE_CHECK_RESULT).
 * 회차별 (run_at, market, strategy_type) 메트릭·열화 여부 저장.
 */
@Entity
@Table(name = "TB_GOVERNANCE_CHECK_RESULT", indexes = {
        @Index(name = "IDX_TB_GOVERNANCE_CHECK_RESULT_RUN_AT", columnList = "RUN_AT"),
        @Index(name = "IDX_TB_GOVERNANCE_CHECK_RESULT_MARKET_TYPE", columnList = "MARKET, STRATEGY_TYPE")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GovernanceCheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "RUN_AT", nullable = false)
    private Instant runAt;

    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "STRATEGY_TYPE", nullable = false, length = 32)
    private String strategyType;

    @Column(name = "MDD_PCT", precision = 10, scale = 4)
    private BigDecimal mddPct;

    @Column(name = "SHARPE_RATIO", precision = 10, scale = 4)
    private BigDecimal sharpeRatio;

    @Column(name = "DEGRADED", nullable = false, length = 1)
    private String degraded;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    public static GovernanceCheckResult of(Instant runAt, String market, String strategyType,
                                          BigDecimal mddPct, BigDecimal sharpeRatio, boolean degraded,
                                          LocalDate startDate, LocalDate endDate) {
        GovernanceCheckResult e = new GovernanceCheckResult();
        e.runAt = runAt != null ? runAt : Instant.now();
        e.market = market;
        e.strategyType = strategyType;
        e.mddPct = mddPct;
        e.sharpeRatio = sharpeRatio;
        e.degraded = degraded ? "Y" : "N";
        e.startDate = startDate;
        e.endDate = endDate;
        e.createdAt = Instant.now();
        return e;
    }
}
