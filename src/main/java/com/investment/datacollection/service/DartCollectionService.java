package com.investment.datacollection.service;

import com.investment.datacollection.client.DartApiClient;
import com.investment.datacollection.dto.DartListItemDto;
import com.investment.domain.entity.NewsItem;
import com.investment.news.service.NewsItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Open DART 공시 수집 → NewsItem 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DartCollectionService {

    private static final String SOURCE_DART = "DART";
    private static final String MARKET_KR = "KR";
    private static final String ITEM_TYPE_FACT = "FACT";
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DartApiClient dartApiClient;
    private final NewsItemService newsItemService;

    @Value("${investment.data.dart.collect-days:3}")
    private int collectDays = 3;

    /**
     * 최근 N일 공시 수집 후 중복 제외 저장
     *
     * @return 저장된 건수
     */
    @Transactional
    public int collectAndSave() {
        List<DartListItemDto> items = dartApiClient.fetchListForDays(collectDays);
        int saved = 0;
        for (DartListItemDto dto : items) {
            try {
                NewsItem item = toNewsItem(dto);
                if (newsItemService.saveCollectedItem(item)) {
                    saved++;
                }
            } catch (Exception e) {
                log.warn("DART 항목 저장 실패: rceptNo={}, error={}", dto.getRceptNo(), e.getMessage());
            }
        }
        log.info("DART 수집 완료: fetched={}, saved={}", items.size(), saved);
        return saved;
    }

    private NewsItem toNewsItem(DartListItemDto dto) {
        String url = DartApiClient.buildViewerUrl(dto.getRceptNo());
        LocalDateTime collectedAt = parseRceptDt(dto.getRceptDt());
        String title = dto.getReportNm() != null ? dto.getReportNm() : "";
        if (title.length() > 500) {
            title = title.substring(0, 500);
        }
        String summary = buildSummary(dto);
        return NewsItem.builder()
                .source(SOURCE_DART)
                .market(MARKET_KR)
                .itemType(ITEM_TYPE_FACT)
                .title(title)
                .summary(summary)
                .url(url)
                .collectedAt(collectedAt)
                .symbol(dto.getStockCode() != null && !dto.getStockCode().isBlank() ? dto.getStockCode() : null)
                .eventType(dto.getReportNm())
                .build();
    }

    private LocalDateTime parseRceptDt(String rceptDt) {
        if (rceptDt == null || rceptDt.length() < 8) {
            return LocalDateTime.now();
        }
        try {
            LocalDate date = LocalDate.parse(rceptDt.substring(0, 8), DART_DATE);
            return date.atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String buildSummary(DartListItemDto dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getCorpName() != null) {
            sb.append(dto.getCorpName());
        }
        if (dto.getFlrNm() != null && !dto.getFlrNm().isBlank()) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(dto.getFlrNm());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
