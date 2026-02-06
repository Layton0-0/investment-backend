package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Kill Switch: 긴급 시 모든 주문 차단.
 * 단일 행(id=1)만 사용.
 */
@Entity
@Table(name = "TB_TRADING_HALT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingHalt {

    @Id
    @Column(name = "ID", nullable = false)
    private Integer id = 1;

    @Column(name = "HALT_ALL_ORDERS", nullable = false)
    private Boolean haltAllOrders = false;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static TradingHalt createDefault() {
        TradingHalt h = new TradingHalt();
        h.id = 1;
        h.haltAllOrders = false;
        h.updatedAt = LocalDateTime.now();
        return h;
    }

    public void setHaltAllOrders(boolean halt) {
        this.haltAllOrders = halt;
    }
}
