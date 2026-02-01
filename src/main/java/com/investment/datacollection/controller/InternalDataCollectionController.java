package com.investment.datacollection.controller;

import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.dto.CollectedNewsItemDto;
import com.investment.datacollection.dto.CollectedNewsRequestDto;
import com.investment.domain.entity.NewsItem;
import com.investment.news.service.NewsItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 내부 데이터 수집 API (Yahoo 등 Python 수집기 → Spring 전달)
 * X-Internal-Data-Key 헤더로 보호. investment.data.internal-api-key 와 일치해야 함.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalDataCollectionController {

    private static final String HEADER_INTERNAL_KEY = "X-Internal-Data-Key";

    private final NewsItemService newsItemService;
    private final DataCollectionProperties dataCollectionProperties;

    /**
     * 수집 뉴스·이벤트 일괄 등록 (Yahoo Earnings Calendar 등)
     * 헤더 X-Internal-Data-Key 가 설정된 internal-api-key 와 일치할 때만 처리.
     */
    @PostMapping("/collected-news")
    public ResponseEntity<CollectedNewsResponse> postCollectedNews(
            @RequestHeader(value = HEADER_INTERNAL_KEY, required = false) String internalKey,
            @RequestBody(required = false) CollectedNewsRequestDto request) {
        String expectedKey = dataCollectionProperties.getInternalApiKey();
        if (expectedKey == null || expectedKey.isBlank()) {
            log.warn("내부 수집 API 비활성화: internal-api-key 미설정");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (!expectedKey.equals(internalKey)) {
            log.warn("내부 수집 API 인증 실패: 키 불일치");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.ok(CollectedNewsResponse.of(0, 0));
        }

        int saved = 0;
        for (CollectedNewsItemDto dto : request.getItems()) {
            try {
                NewsItem item = toNewsItem(dto);
                if (newsItemService.saveCollectedItem(item)) {
                    saved++;
                }
            } catch (Exception e) {
                log.warn("수집 항목 저장 실패: source={}, url={}, error={}", dto.getSource(), dto.getUrl(), e.getMessage());
            }
        }
        log.info("내부 수집 API 처리: received={}, saved={}", request.getItems().size(), saved);
        return ResponseEntity.ok(CollectedNewsResponse.of(request.getItems().size(), saved));
    }

    private NewsItem toNewsItem(CollectedNewsItemDto dto) {
        LocalDateTime collectedAt = dto.getCollectedAt() != null ? dto.getCollectedAt() : LocalDateTime.now();
        String title = dto.getTitle() != null ? dto.getTitle() : "";
        if (title.length() > 500) {
            title = title.substring(0, 500);
        }
        return NewsItem.builder()
                .source(dto.getSource() != null ? dto.getSource() : "YAHOO_FINANCE")
                .market(dto.getMarket() != null ? dto.getMarket() : "US")
                .itemType(dto.getItemType() != null ? dto.getItemType() : "BUZZ")
                .title(title)
                .summary(dto.getSummary())
                .url(dto.getUrl() != null ? dto.getUrl() : "")
                .collectedAt(collectedAt)
                .symbol(dto.getSymbol())
                .eventType(NewsItem.truncateEventType(dto.getEventType()))
                .build();
    }

    @lombok.Getter
    @lombok.AllArgsConstructor(staticName = "of")
    public static class CollectedNewsResponse {
        private final int received;
        private final int saved;
    }
}
