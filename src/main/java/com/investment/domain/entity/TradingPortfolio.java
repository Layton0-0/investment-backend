package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 트레이딩 포트폴리오 엔티티 (일별 트레이딩 계획)
 */
@Entity
@Table(name = "TB_TRADING_PORTFOLIOS", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"TRADING_DATE"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradingPortfolio {
    
    @Id
    @Column(name = "TB_TRADING_PORTFOLIOS_UID", length = 36, nullable = false, unique = true)
    private String id;
    
    @Column(name = "TRADING_DATE", nullable = false, unique = true)
    private LocalDate tradingDate;
    
    @Column(name = "MARKET_SUMMARY", columnDefinition = "TEXT")
    private String marketSummary;
    
    @Column(name = "TOP_SECTOR_1", length = 100)
    private String topSector1;
    
    @Column(name = "TOP_SECTOR_2", length = 100)
    private String topSector2;
    
    @Column(name = "TOP_SECTOR_3", length = 100)
    private String topSector3;
    
    @Column(name = "RISK_MANAGEMENT_STRATEGY", columnDefinition = "TEXT")
    private String riskManagementStrategy;
    
    @Column(name = "POSITION_SIZE", precision = 18, scale = 2)
    private BigDecimal positionSize;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "tradingPortfolio", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TradingPortfolioItem> items = new ArrayList<>();
    
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
    public TradingPortfolio(LocalDate tradingDate, String marketSummary, 
                           String topSector1, String topSector2, String topSector3,
                           String riskManagementStrategy, BigDecimal positionSize) {
        this.tradingDate = tradingDate;
        this.marketSummary = marketSummary;
        this.topSector1 = topSector1;
        this.topSector2 = topSector2;
        this.topSector3 = topSector3;
        this.riskManagementStrategy = riskManagementStrategy;
        this.positionSize = positionSize;
    }
    
    public void addItem(TradingPortfolioItem item) {
        items.add(item);
        item.setTradingPortfolio(this);
    }
    
    public void updateMarketSummary(String marketSummary) {
        this.marketSummary = marketSummary;
    }
    
    public void updateTopSectors(String sector1, String sector2, String sector3) {
        this.topSector1 = sector1;
        this.topSector2 = sector2;
        this.topSector3 = sector3;
    }
    
    public void updateRiskManagementStrategy(String strategy) {
        this.riskManagementStrategy = strategy;
    }
}
