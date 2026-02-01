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
 * 재무 데이터 (TB_FUNDAMENTALS).
 * PEG & Rule of 40(미국) 시그널용.
 */
@Entity
@Table(name = "TB_FUNDAMENTALS", indexes = {
        @Index(name = "IDX_TB_FUNDAMENTALS_BAS_DT_MARKET", columnList = "BAS_DT, MARKET")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(FundamentalsId.class)
public class Fundamentals {

    @Id
    @Column(name = "BAS_DT", nullable = false)
    private LocalDate basDt;

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "PER", precision = 12, scale = 4)
    private BigDecimal per;

    @Column(name = "PEG", precision = 12, scale = 4)
    private BigDecimal peg;

    @Column(name = "REVENUE_GROWTH_PCT", precision = 12, scale = 4)
    private BigDecimal revenueGrowthPct;

    @Column(name = "OPERATING_MARGIN_PCT", precision = 12, scale = 4)
    private BigDecimal operatingMarginPct;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public Fundamentals(LocalDate basDt, String symbol, String market,
                        BigDecimal per, BigDecimal peg, BigDecimal revenueGrowthPct, BigDecimal operatingMarginPct,
                        LocalDateTime createdAt) {
        this.basDt = basDt;
        this.symbol = symbol;
        this.market = market;
        this.per = per;
        this.peg = peg;
        this.revenueGrowthPct = revenueGrowthPct;
        this.operatingMarginPct = operatingMarginPct;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
