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
 * 업종별 수익률 (TB_SECTOR_RETURN).
 * Sector Relative Strength(한국) 유니버스 필터용.
 */
@Entity
@Table(name = "TB_SECTOR_RETURN", indexes = {
        @Index(name = "IDX_TB_SECTOR_RETURN_BAS_DT_MARKET", columnList = "BAS_DT, MARKET")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(SectorReturnId.class)
public class SectorReturn {

    @Id
    @Column(name = "BAS_DT", nullable = false)
    private LocalDate basDt;

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Id
    @Column(name = "SECTOR_CODE", nullable = false, length = 50)
    private String sectorCode;

    @Column(name = "RETURN_PCT", precision = 12, scale = 4)
    private BigDecimal returnPct;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public SectorReturn(LocalDate basDt, String market, String sectorCode,
                        BigDecimal returnPct, LocalDateTime createdAt) {
        this.basDt = basDt;
        this.market = market;
        this.sectorCode = sectorCode;
        this.returnPct = returnPct;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
