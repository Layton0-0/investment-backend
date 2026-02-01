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
 * 뉴스·공시 수집 항목 (TB_NEWS_ITEMS)
 * 공시/데이터(Fact)·뉴스/속보(Speed)·센티멘트/수급(Buzz) 원천 연동용.
 */
@Entity
@Table(name = "TB_NEWS_ITEMS", indexes = {
        @Index(name = "IDX_TB_NEWS_ITEMS_MARKET", columnList = "MARKET"),
        @Index(name = "IDX_TB_NEWS_ITEMS_COLLECTED_AT", columnList = "COLLECTED_AT"),
        @Index(name = "IDX_TB_NEWS_ITEMS_SYMBOL", columnList = "SYMBOL")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsItem {

    /** EVENT_TYPE 컬럼 최대 길이 (DB VARCHAR(500)과 동일) */
    public static final int MAX_EVENT_TYPE_LENGTH = 500;

    /**
     * EVENT_TYPE 저장 시 길이 제한. null이면 null, 초과 시 앞쪽만 반환.
     */
    public static String truncateEventType(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= MAX_EVENT_TYPE_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_EVENT_TYPE_LENGTH);
    }

    @Id
    @Column(name = "TB_NEWS_ITEMS_UID", length = 36, nullable = false, unique = true)
    private String id;

    @Column(name = "SOURCE", nullable = false, length = 50)
    private String source;

    @Column(name = "MARKET", nullable = false, length = 10)
    private String market;

    @Column(name = "ITEM_TYPE", nullable = false, length = 20)
    private String itemType;

    @Column(name = "TITLE", nullable = false, length = 500)
    private String title;

    @Column(name = "SUMMARY", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "URL", nullable = false, length = 1000)
    private String url;

    @Column(name = "COLLECTED_AT", nullable = false)
    private LocalDateTime collectedAt;

    @Column(name = "SYMBOL", length = 20)
    private String symbol;

    @Column(name = "SENTIMENT_SCORE", precision = 5, scale = 2)
    private BigDecimal sentimentScore;

    @Column(name = "IMPORTANCE_SCORE", precision = 5, scale = 2)
    private BigDecimal importanceScore;

    @Column(name = "EVENT_TYPE", length = MAX_EVENT_TYPE_LENGTH)
    private String eventType;

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
    public NewsItem(String source, String market, String itemType, String title, String summary,
            String url, LocalDateTime collectedAt, String symbol,
            BigDecimal sentimentScore, BigDecimal importanceScore, String eventType) {
        this.source = source;
        this.market = market;
        this.itemType = itemType;
        this.title = title;
        this.summary = summary;
        this.url = url;
        this.collectedAt = collectedAt != null ? collectedAt : LocalDateTime.now();
        this.symbol = symbol;
        this.sentimentScore = sentimentScore;
        this.importanceScore = importanceScore;
        this.eventType = eventType;
    }
}
