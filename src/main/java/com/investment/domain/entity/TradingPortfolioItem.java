package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * 트레이딩 포트폴리오 종목 엔티티
 */
@Entity
@Table(name = "TB_TRADING_PORTFOLIO_ITEMS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingPortfolioItem {
    
    @Id
    @Column(name = "TB_TRADING_PORTFOLIO_ITEMS_UID", length = 36, nullable = false, unique = true)
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRADING_PORTFOLIO_ID", nullable = false)
    @Setter
    private TradingPortfolio tradingPortfolio;
    
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;
    
    @Column(name = "NAME", length = 100)
    private String name;
    
    @Column(name = "ENTRY_PRICE_MIN", precision = 18, scale = 2)
    private BigDecimal entryPriceMin;
    
    @Column(name = "ENTRY_PRICE_MAX", precision = 18, scale = 2)
    private BigDecimal entryPriceMax;
    
    @Column(name = "STOP_LOSS_PRICE", precision = 18, scale = 2)
    private BigDecimal stopLossPrice;
    
    @Column(name = "TARGET_PRICE_1", precision = 18, scale = 2)
    private BigDecimal targetPrice1;
    
    @Column(name = "TARGET_PRICE_2", precision = 18, scale = 2)
    private BigDecimal targetPrice2;
    
    @Column(name = "EXPECTED_RETURN_RATE", precision = 5, scale = 2)
    private BigDecimal expectedReturnRate;
    
    @Column(name = "RISK_REWARD_RATIO", precision = 5, scale = 2)
    private BigDecimal riskRewardRatio;
    
    @Column(name = "TECHNICAL_BASIS", columnDefinition = "TEXT")
    private String technicalBasis;
    
    @Column(name = "SUPPLY_DEMAND_BASIS", columnDefinition = "TEXT")
    private String supplyDemandBasis;
    
    @Column(name = "CATALYST_FACTOR", columnDefinition = "TEXT")
    private String catalystFactor;
    
    @Column(name = "BUY_TIME", nullable = false)
    private LocalTime buyTime;
    
    @Column(name = "SELL_TIME")
    private LocalTime sellTime;
    
    @Column(name = "INVESTMENT_AMOUNT", precision = 18, scale = 2)
    private BigDecimal investmentAmount;
    
    @Column(name = "EXPECTED_PROFIT", precision = 18, scale = 2)
    private BigDecimal expectedProfit;
    
    @Column(name = "RANKING", nullable = false)
    private Integer ranking;
    
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
    public TradingPortfolioItem(String symbol, String name,
                                BigDecimal entryPriceMin, BigDecimal entryPriceMax,
                                BigDecimal stopLossPrice, BigDecimal targetPrice1, BigDecimal targetPrice2,
                                BigDecimal expectedReturnRate, BigDecimal riskRewardRatio,
                                String technicalBasis, String supplyDemandBasis, String catalystFactor,
                                LocalTime buyTime, LocalTime sellTime,
                                BigDecimal investmentAmount, BigDecimal expectedProfit,
                                Integer ranking) {
        this.symbol = symbol;
        this.name = name;
        this.entryPriceMin = entryPriceMin;
        this.entryPriceMax = entryPriceMax;
        this.stopLossPrice = stopLossPrice;
        this.targetPrice1 = targetPrice1;
        this.targetPrice2 = targetPrice2;
        this.expectedReturnRate = expectedReturnRate;
        this.riskRewardRatio = riskRewardRatio;
        this.technicalBasis = technicalBasis;
        this.supplyDemandBasis = supplyDemandBasis;
        this.catalystFactor = catalystFactor;
        this.buyTime = buyTime;
        this.sellTime = sellTime;
        this.investmentAmount = investmentAmount;
        this.expectedProfit = expectedProfit;
        this.ranking = ranking;
    }
}
