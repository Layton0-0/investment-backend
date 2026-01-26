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
 * 포트폴리오 엔티티 (보유 종목)
 */
@Entity
@Table(name = "TB_PORTFOLIOS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {
    
    @Id
    @Column(name = "TB_PORTFOLIOS_UID", length = 36, nullable = false, unique = true)
    private String id;
    
    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;
    
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;
    
    @Column(name = "NAME", length = 100)
    private String name;
    
    @Column(name = "QUANTITY", nullable = false)
    private Integer quantity;
    
    @Column(name = "AVERAGE_PRICE", nullable = false, precision = 18, scale = 2)
    private BigDecimal averagePrice;
    
    @Column(name = "CURRENT_PRICE", precision = 18, scale = 2)
    private BigDecimal currentPrice;
    
    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;
    
    @Column(name = "LAST_UPDATED")
    private LocalDateTime lastUpdated;
    
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
        if (lastUpdated == null) {
            lastUpdated = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Builder
    public Portfolio(String accountNo, String symbol, String name, 
                    Integer quantity, BigDecimal averagePrice, String currency) {
        this.accountNo = accountNo;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.currency = currency;
    }
    
    public void updateCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void addQuantity(Integer quantity, BigDecimal price) {
        int totalQuantity = this.quantity + quantity;
        BigDecimal totalValue = this.averagePrice.multiply(BigDecimal.valueOf(this.quantity))
                .add(price.multiply(BigDecimal.valueOf(quantity)));
        this.averagePrice = totalValue.divide(BigDecimal.valueOf(totalQuantity), 2, BigDecimal.ROUND_HALF_UP);
        this.quantity = totalQuantity;
    }
    
    public void subtractQuantity(Integer quantity) {
        this.quantity -= quantity;
        if (this.quantity < 0) {
            this.quantity = 0;
        }
    }
}
