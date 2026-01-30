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
 * 시그널/팩터 점수 (TB_SIGNAL_SCORE)
 * 팩터 계산 엔진 산출물 저장.
 */
@Entity
@Table(name = "TB_SIGNAL_SCORE", indexes = {
        @Index(name = "IDX_TB_SIGNAL_SCORE_BAS_DT", columnList = "BAS_DT"),
        @Index(name = "IDX_TB_SIGNAL_SCORE_SYMBOL", columnList = "SYMBOL"),
        @Index(name = "IDX_TB_SIGNAL_SCORE_MARKET", columnList = "MARKET"),
        @Index(name = "IDX_TB_SIGNAL_SCORE_FACTOR_TYPE", columnList = "FACTOR_TYPE"),
        @Index(name = "IDX_TB_SIGNAL_SCORE_BAS_DT_MARKET", columnList = "BAS_DT, MARKET")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(SignalScoreId.class)
public class SignalScore {

    @Id
    @Column(name = "BAS_DT", nullable = false)
    private LocalDate basDt;

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(name = "FACTOR_TYPE", nullable = false, length = 50)
    private String factorType;

    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "SCORE", precision = 20, scale = 6)
    private BigDecimal score;

    @Column(name = "METADATA", length = 500)
    private String metadata;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public SignalScore(LocalDate basDt, String symbol, String factorType, String market,
            BigDecimal score, String metadata, LocalDateTime createdAt) {
        this.basDt = basDt;
        this.symbol = symbol;
        this.factorType = factorType;
        this.market = market != null ? market : "KR";
        this.score = score;
        this.metadata = metadata;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
