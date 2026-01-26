package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 주문 엔티티
 */
@Entity
@Table(name = "TB_ORDERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    
    @Id
    @Column(name = "TB_ORDERS_UID", length = 36, nullable = false, unique = true)
    private String id;
    
    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;
    
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
    public Order(String accountNo, String symbol, OrderType orderType, 
                 Integer quantity, BigDecimal price, OrderStatus status) {
        this.accountNo = accountNo;
        this.symbol = symbol;
        this.orderType = orderType;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
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
    
    public enum OrderType {
        BUY, SELL
    }
    
    public enum OrderStatus {
        PENDING, EXECUTED, PARTIAL, CANCELLED, FAILED
    }
}
