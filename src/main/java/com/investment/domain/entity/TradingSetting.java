package com.investment.domain.entity;

import lombok.*;

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
     * -- SETTER --
     *  사용자 ID 설정

     */
    @Setter
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

    /**
     * 로보 어드바이저 사용 여부. true면 로보 리밸런싱 스케줄러 대상 (V17).
     */
    @Column(name = "ROBO_ADVISOR_ENABLED", nullable = false)
    private Boolean roboAdvisorEnabled = false;

    @Column(name = "RISK_LEVEL", precision = 3, scale = 2)
    private BigDecimal riskLevel;

    /**
     * 단기 비율 (0~1). NULL이면 스케줄러 기본값 0.2 사용.
     */
    @Column(name = "SHORT_TERM_RATIO", precision = 5, scale = 4)
    private BigDecimal shortTermRatio;

    /**
     * 중기 비율 (0~1). NULL이면 스케줄러 기본값 0.4 사용.
     */
    @Column(name = "MEDIUM_TERM_RATIO", precision = 5, scale = 4)
    private BigDecimal mediumTermRatio;

    /**
     * 장기 비율 (0~1). NULL이면 스케줄러 기본값 0.4 사용.
     */
    @Column(name = "LONG_TERM_RATIO", precision = 5, scale = 4)
    private BigDecimal longTermRatio;

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
            Boolean autoTradingEnabled, Boolean roboAdvisorEnabled, BigDecimal riskLevel,
            BigDecimal shortTermRatio, BigDecimal mediumTermRatio, BigDecimal longTermRatio) {
        this.accountNo = accountNo;
        this.userId = userId;
        this.maxInvestmentAmount = maxInvestmentAmount;
        this.minInvestmentAmount = minInvestmentAmount;
        this.defaultCurrency = defaultCurrency;
        this.autoTradingEnabled = autoTradingEnabled != null ? autoTradingEnabled : false;
        this.roboAdvisorEnabled = Boolean.TRUE.equals(roboAdvisorEnabled);
        this.riskLevel = riskLevel;
        this.shortTermRatio = shortTermRatio;
        this.mediumTermRatio = mediumTermRatio;
        this.longTermRatio = longTermRatio;
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

    public void updateRoboAdvisorEnabled(Boolean enabled) {
        this.roboAdvisorEnabled = Boolean.TRUE.equals(enabled);
    }

    public void updateRiskLevel(BigDecimal riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void updateStrategyRatios(BigDecimal shortTermRatio, BigDecimal mediumTermRatio, BigDecimal longTermRatio) {
        this.shortTermRatio = shortTermRatio;
        this.mediumTermRatio = mediumTermRatio;
        this.longTermRatio = longTermRatio;
    }
}
