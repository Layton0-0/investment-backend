package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 유니버스(감시 대상 종목) — TB_UNIVERSE.
 * 1단계 유니버스 필터링 통과 종목만 저장.
 */
@Entity
@Table(name = "TB_UNIVERSE", indexes = {
        @Index(name = "IDX_TB_UNIVERSE_BAS_DT", columnList = "BAS_DT"),
        @Index(name = "IDX_TB_UNIVERSE_MARKET", columnList = "MARKET"),
        @Index(name = "IDX_TB_UNIVERSE_BAS_DT_MARKET", columnList = "BAS_DT, MARKET")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(UniverseId.class)
public class Universe {

    @Id
    @Column(name = "BAS_DT", nullable = false)
    private LocalDate basDt;

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public Universe(LocalDate basDt, String market, String symbol, LocalDateTime createdAt) {
        this.basDt = basDt;
        this.market = market != null ? market : "KR";
        this.symbol = symbol;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
