package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 엔티티
 */
@Entity
@Table(name = "TB_ORDERS", indexes = {
        @Index(name = "IDX_TB_ORDERS_USER_ID", columnList = "USER_ID")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @Column(name = "TB_ORDERS_UID", length = 36, nullable = false, unique = true)
    private String id;

    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;

    /**
     * 사용자 ID (선택적, 점진적 마이그레이션용)
     * 하위 호환성을 위해 NULL 허용
     */
    @Column(name = "USER_ID", length = 36)
    private String userId;

    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE", nullable = false, length = 10)
    private OrderType orderType;

    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;

    @Column(name = "PRICE", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "EXECUTED_QUANTITY")
    private Integer executedQuantity;

    @Column(name = "EXECUTED_PRICE", precision = 18, scale = 2)
    private BigDecimal executedPrice;

    @Column(name = "ORDER_TIME", nullable = false)
    private LocalDateTime orderTime;

    @Column(name = "EXECUTED_TIME")
    private LocalDateTime executedTime;

    @Column(name = "MESSAGE", length = 500)
    private String message;

    /** 체결 확인 후 포지션 등록 시 사용 (기준일·시장·전략타입) */
    @Column(name = "POSITION_BAS_DT")
    private LocalDate positionBasDt;

    @Column(name = "POSITION_MARKET", length = 10)
    private String positionMarket;

    @Column(name = "POSITION_STRATEGY_TYPE", length = 20)
    private String positionStrategyType;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderTime == null) {
            orderTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Order(String accountNo, String userId, String symbol, OrderType orderType,
            Integer quantity, BigDecimal price, OrderStatus status) {
        this.accountNo = accountNo;
        this.userId = userId;
        this.symbol = symbol;
        this.orderType = orderType;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    /**
     * 사용자 ID 설정
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public void execute(Integer executedQuantity, BigDecimal executedPrice) {
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.executedTime = LocalDateTime.now();
        this.status = OrderStatus.EXECUTED;
    }

    public void execute(Integer executedQuantity, BigDecimal executedPrice, String message) {
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.executedTime = LocalDateTime.now();
        this.status = OrderStatus.EXECUTED;
        this.message = message;
    }

    public void partialExecute(Integer executedQuantity, BigDecimal executedPrice) {
        this.executedQuantity = executedQuantity;
        this.executedPrice = executedPrice;
        this.executedTime = LocalDateTime.now();
        this.status = OrderStatus.PARTIAL;
    }

    public void cancel(String message) {
        this.status = OrderStatus.CANCELLED;
        this.message = message;
    }

    public void fail(String message) {
        this.status = OrderStatus.FAILED;
        this.message = message;
    }

    /**
     * 체결 확인 후 포지션 등록 시 사용할 컨텍스트 설정.
     */
    public void setPositionContext(LocalDate positionBasDt, String positionMarket, String positionStrategyType) {
        this.positionBasDt = positionBasDt;
        this.positionMarket = positionMarket;
        this.positionStrategyType = positionStrategyType;
    }

    /**
     * 포지션 등록 완료 후 컨텍스트 초기화.
     */
    public void clearPositionContext() {
        this.positionBasDt = null;
        this.positionMarket = null;
        this.positionStrategyType = null;
    }

    public enum OrderType {
        BUY, SELL
    }

    public enum OrderStatus {
        PENDING, EXECUTED, PARTIAL, CANCELLED, FAILED
    }
}
