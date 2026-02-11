package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 일별 시세 (TB_DAILY_STOCK).
 * 팩터 계산·백테스트 입력용 OHLCV·거래대금 저장.
 * <p>저장되는 가격은 수정주가(adjusted price) 정책을 따른다. 원주가는 차트 표시 등에만 사용 가능.
 * KR: KRX 일별매매정보 또는 한투 API(FID_ORG_ADJ_PRC=0), US: yfinance adjusted. ADR 19 참조.
 */
@Entity
@Table(name = "TB_DAILY_STOCK", indexes = {
        @Index(name = "IDX_TB_DAILY_STOCK_BAS_DT", columnList = "BAS_DT"),
        @Index(name = "IDX_TB_DAILY_STOCK_SYMBOL", columnList = "SYMBOL"),
        @Index(name = "IDX_TB_DAILY_STOCK_MARKET", columnList = "MARKET"),
        @Index(name = "IDX_TB_DAILY_STOCK_BAS_DT_SYMBOL", columnList = "BAS_DT, SYMBOL")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(DailyStockId.class)
public class DailyStock {

    @Id
    @Column(name = "BAS_DT", nullable = false)
    private LocalDate basDt;

    @Id
    @Column(name = "SYMBOL", nullable = false, length = 20)
    private String symbol;

    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "OPEN_PRICE", precision = 20, scale = 4)
    private BigDecimal openPrice;

    @Column(name = "HIGH_PRICE", precision = 20, scale = 4)
    private BigDecimal highPrice;

    @Column(name = "LOW_PRICE", precision = 20, scale = 4)
    private BigDecimal lowPrice;

    @Column(name = "CLOSE_PRICE", precision = 20, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "VOLUME")
    private Long volume;

    @Column(name = "TRD_VAL")
    private Long trdVal;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    @Builder
    public DailyStock(LocalDate basDt, String symbol, String market,
            BigDecimal openPrice, BigDecimal highPrice, BigDecimal lowPrice, BigDecimal closePrice,
            Long volume, Long trdVal, LocalDateTime createdAt) {
        this.basDt = basDt;
        this.symbol = symbol;
        this.market = market != null ? market : "KR";
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.closePrice = closePrice;
        this.volume = volume;
        this.trdVal = trdVal;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
    }
}
