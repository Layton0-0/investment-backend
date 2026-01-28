package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 거래 설정 엔티티
 */
@Entity
@Table(name = "TB_TRADING_SETTINGS", indexes = {
        @Index(name = "IDX_TB_TRADING_SETTINGS_USER_ID", columnList = "USER_ID"),
        @Index(name = "UK_TB_TRADING_SETTINGS_USER_ACCOUNT", columnList = "USER_ID,ACCOUNT_NO", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingSetting {

    @Id
    @Column(name = "TB_TRADING_SETTINGS_UID", length = 36, nullable = false, unique = true)
    private String id;

    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;

    /**
     * 사용자 ID (사용자별 설정 관리용)
     * 하위 호환성을 위해 NULL 허용
     */
    @Column(name = "USER_ID", length = 36)
    private String userId;

    @Column(name = "MAX_INVESTMENT_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal maxInvestmentAmount;

    @Column(name = "MIN_INVESTMENT_AMOUNT", nullable = false, precision = 18, scale = 2)
    private BigDecimal minInvestmentAmount;

    @Column(name = "DEFAULT_CURRENCY", nullable = false, length = 3)
    private String defaultCurrency;

    @Column(name = "AUTO_TRADING_ENABLED", nullable = false)
    private Boolean autoTradingEnabled;

    @Column(name = "RISK_LEVEL", precision = 3, scale = 2)
    private BigDecimal riskLevel;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public TradingSetting(String accountNo, String userId, BigDecimal maxInvestmentAmount,
            BigDecimal minInvestmentAmount, String defaultCurrency,
            Boolean autoTradingEnabled, BigDecimal riskLevel) {
        this.accountNo = accountNo;
        this.userId = userId;
        this.maxInvestmentAmount = maxInvestmentAmount;
        this.minInvestmentAmount = minInvestmentAmount;
        this.defaultCurrency = defaultCurrency;
        this.autoTradingEnabled = autoTradingEnabled != null ? autoTradingEnabled : false;
        this.riskLevel = riskLevel;
    }

    /**
     * 사용자 ID 설정
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void updateMaxInvestmentAmount(BigDecimal maxInvestmentAmount) {
        this.maxInvestmentAmount = maxInvestmentAmount;
    }

    public void updateMinInvestmentAmount(BigDecimal minInvestmentAmount) {
        this.minInvestmentAmount = minInvestmentAmount;
    }

    public void updateAutoTradingEnabled(Boolean enabled) {
        this.autoTradingEnabled = enabled;
    }

    public void updateRiskLevel(BigDecimal riskLevel) {
        this.riskLevel = riskLevel;
    }
}
