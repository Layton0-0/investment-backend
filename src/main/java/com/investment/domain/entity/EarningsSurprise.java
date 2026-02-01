package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 어닝 서프라이즈 (TB_EARNINGS_SURPRISE).
 * Post-Earnings Drift(미국) 유니버스 필터용.
 */
@Entity
@Table(name = "TB_EARNINGS_SURPRISE", indexes = {
        @Index(name = "IDX_TB_EARNINGS_SURPRISE_MARKET_REPORT_DT", columnList = "MARKET, REPORT_DT")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(EarningsSurpriseId.class)
public class EarningsSurprise {

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Id
    @Column(name = "REPORT_DT", nullable = false)
    private LocalDate reportDt;

    @Column(name = "SURPRISE_SCORE", precision = 12, scale = 4)
    private BigDecimal surpriseScore;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public EarningsSurprise(String symbol, String market, LocalDate reportDt,
                            BigDecimal surpriseScore, LocalDateTime createdAt) {
        this.symbol = symbol;
        this.market = market;
        this.reportDt = reportDt;
        this.surpriseScore = surpriseScore;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
