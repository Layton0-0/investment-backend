package com.investment.datacollection.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 수집 뉴스·이벤트 항목 (내부 API 수신용)
 * Yahoo 등 외부 수집기에서 POST /api/v1/internal/collected-news 로 전달.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectedNewsItemDto {

    private String source;
    private String market;
    private String itemType;
    private String title;
    private String summary;
    private String url;
    private LocalDateTime collectedAt;
    private String symbol;
    private String eventType;
    /** DART 키워드 매칭 등 시그널 반영 대상 여부 (optional) */
    private Boolean signalRelevant;
}
