package com.investment.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 뉴스·공시 항목 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsItemDto {

    private String id;
    private String source;
    private String market;
    private String itemType;
    private String title;
    private String summary;
    private String url;
    private LocalDateTime collectedAt;
    private String symbol;
    private BigDecimal sentimentScore;
    private BigDecimal importanceScore;
    private String eventType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
