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
 * 파이프라인 매수 포지션 — TB_STRATEGY_POSITION.
 * 4단계 실행·청산 규칙(ATR Trailing Stop, Time-Cut) 추적용.
 */
@Entity
@Table(name = "TB_STRATEGY_POSITION", indexes = {
        @Index(name = "IDX_TB_STRATEGY_POSITION_ACCOUNT", columnList = "ACCOUNT_NO"),
        @Index(name = "IDX_TB_STRATEGY_POSITION_SYMBOL", columnList = "SYMBOL"),
        @Index(name = "IDX_TB_STRATEGY_POSITION_OPEN", columnList = "ACCOUNT_NO, EXIT_DT")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrategyPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;

    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "ENTRY_DT", nullable = false)
    private LocalDate entryDt;

    @Column(name = "ENTRY_PRICE", nullable = false, precision = 20, scale = 4)
    private BigDecimal entryPrice;

    @Column(name = "QUANTITY", nullable = false)
    private int quantity;

    @Column(name = "TRAILING_HIGH", precision = 20, scale = 4)
    private BigDecimal trailingHigh;

    @Column(name = "ATR_MULTIPLIER", nullable = false, precision = 10, scale = 4)
    private BigDecimal atrMultiplier;

    @Column(name = "TIME_CUT_DAYS", nullable = false)
    private int timeCutDays;

    @Column(name = "TARGET_RETURN_PCT", precision = 10, scale = 4)
    private BigDecimal targetReturnPct;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "EXIT_DT")
    private LocalDate exitDt;

    @Column(name = "EXIT_PRICE", precision = 20, scale = 4)
    private BigDecimal exitPrice;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public StrategyPosition(Long id, String accountNo, String symbol, String market,
            LocalDate entryDt, BigDecimal entryPrice, int quantity, BigDecimal trailingHigh,
            BigDecimal atrMultiplier, int timeCutDays, BigDecimal targetReturnPct,
            LocalDateTime createdAt, LocalDate exitDt, BigDecimal exitPrice) {
        this.id = id;
        this.accountNo = accountNo;
        this.symbol = symbol;
        this.market = market != null ? market : "KR";
        this.entryDt = entryDt;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.trailingHigh = trailingHigh;
        this.atrMultiplier = atrMultiplier != null ? atrMultiplier : new BigDecimal("2.0");
        this.timeCutDays = timeCutDays;
        this.targetReturnPct = targetReturnPct;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.exitDt = exitDt;
        this.exitPrice = exitPrice;
    }

    public boolean isOpen() {
        return exitDt == null;
    }

    public void close(LocalDate exitDt, BigDecimal exitPrice) {
        this.exitDt = exitDt;
        this.exitPrice = exitPrice;
    }

    public void updateTrailingHigh(BigDecimal high) {
        if (high != null && (trailingHigh == null || high.compareTo(trailingHigh) > 0)) {
            this.trailingHigh = high;
        }
    }
}
