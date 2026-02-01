package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 수급 데이터 (TB_ORDER_FLOW).
 * Smart Money Intensity(한국) 시그널용.
 */
@Entity
@Table(name = "TB_ORDER_FLOW", indexes = {
        @Index(name = "IDX_TB_ORDER_FLOW_BAS_DT_MARKET", columnList = "BAS_DT, MARKET")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(OrderFlowId.class)
public class OrderFlow {

    @Id
    @Column(name = "BAS_DT", nullable = false)
    private LocalDate basDt;

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Id
    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "NET_BUY_AMT_5D")
    private Long netBuyAmt5d;

    @Column(name = "MARKET_CAP")
    private Long marketCap;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public OrderFlow(LocalDate basDt, String symbol, String market, Long netBuyAmt5d, Long marketCap, LocalDateTime createdAt) {
        this.basDt = basDt;
        this.symbol = symbol;
        this.market = market;
        this.netBuyAmt5d = netBuyAmt5d;
        this.marketCap = marketCap;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
