package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 종목-업종 매핑 (TB_SYMBOL_SECTOR).
 * Sector Relative Strength(한국) 유니버스 필터용.
 */
@Entity
@Table(name = "TB_SYMBOL_SECTOR", indexes = {
        @Index(name = "IDX_TB_SYMBOL_SECTOR_MARKET_SECTOR", columnList = "MARKET, SECTOR_CODE")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(SymbolSectorId.class)
public class SymbolSector {

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Id
    @Column(name = "SECTOR_CODE", nullable = false, length = 50)
    private String sectorCode;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public SymbolSector(String symbol, String market, String sectorCode, LocalDateTime createdAt) {
        this.symbol = symbol;
        this.market = market;
        this.sectorCode = sectorCode;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
