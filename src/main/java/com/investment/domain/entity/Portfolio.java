package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 포트폴리오 엔티티 (보유 종목)
 */
@Entity
@Table(name = "TB_PORTFOLIOS", indexes = {
        @Index(name = "IDX_TB_PORTFOLIOS_USER_ID", columnList = "USER_ID")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio {

    @Id
    @Column(name = "TB_PORTFOLIOS_UID", length = 36, nullable = false, unique = true)
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
    public Portfolio(String accountNo, String userId, String symbol, String name,
            Integer quantity, BigDecimal averagePrice, String currency) {
        this.accountNo = accountNo;
        this.userId = userId;
        this.symbol = symbol;
        this.name = name;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.currency = currency;
    }

    /**
     * 사용자 ID 설정
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void updateCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
        this.lastUpdated = LocalDateTime.now();
    }

    public void addQuantity(Integer quantity, BigDecimal price) {
        int totalQuantity = this.quantity + quantity;
        BigDecimal totalValue = this.averagePrice.multiply(BigDecimal.valueOf(this.quantity))
                .add(price.multiply(BigDecimal.valueOf(quantity)));
        this.averagePrice = totalValue.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);
        this.quantity = totalQuantity;
    }

    public void subtractQuantity(Integer quantity) {
        this.quantity -= quantity;
        if (this.quantity < 0) {
            this.quantity = 0;
        }
    }
}
