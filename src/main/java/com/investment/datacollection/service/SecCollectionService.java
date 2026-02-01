package com.investment.datacollection.service;

import com.investment.datacollection.client.SecEdgarApiClient;
import com.investment.datacollection.dto.SecEdgarItemDto;
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
 * SEC EDGAR 공시 수집 → NewsItem 저장 (source=SEC_EDGAR, market=US)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecCollectionService {

    private static final String SOURCE_SEC_EDGAR = "SEC_EDGAR";
    private static final String MARKET_US = "US";
    private static final String ITEM_TYPE_FACT = "FACT";
    private static final DateTimeFormatter SEC_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final SecEdgarApiClient secEdgarApiClient;
    private final NewsItemService newsItemService;

    @Value("${investment.data.sec.collect-days:3}")
    private int collectDays = 3;

    /**
     * 최근 N일 SEC EDGAR 제출 건 수집 후 중복 제외 저장
     *
     * @return 저장된 건수
     */
    @Transactional
    public int collectAndSave() {
        List<SecEdgarItemDto> items = secEdgarApiClient.fetchRecentFilings(collectDays);
        int saved = 0;
        for (SecEdgarItemDto dto : items) {
            try {
                NewsItem item = toNewsItem(dto);
                if (newsItemService.saveCollectedItem(item)) {
                    saved++;
                }
            } catch (Exception e) {
                log.warn("SEC EDGAR 항목 저장 실패: accessionNumber={}, error={}", dto.getAccessionNumber(), e.getMessage());
            }
        }
        log.info("SEC EDGAR 수집 완료: fetched={}, saved={}", items.size(), saved);
        return saved;
    }

    private NewsItem toNewsItem(SecEdgarItemDto dto) {
        String url = SecEdgarApiClient.buildDocumentUrl(dto.getCik(), dto.getAccessionNumber(), dto.getPrimaryDocument());
        LocalDateTime collectedAt = parseFilingDate(dto.getFilingDate());
        String title = buildTitle(dto);
        if (title.length() > 500) {
            title = title.substring(0, 500);
        }
        String summary = buildSummary(dto);
        return NewsItem.builder()
                .source(SOURCE_SEC_EDGAR)
                .market(MARKET_US)
                .itemType(ITEM_TYPE_FACT)
                .title(title)
                .summary(summary)
                .url(url)
                .collectedAt(collectedAt)
                .symbol(null)
                .eventType(NewsItem.truncateEventType(dto.getForm()))
                .build();
    }

    private String buildTitle(SecEdgarItemDto dto) {
        String company = dto.getCompanyName() != null ? dto.getCompanyName() : "";
        String form = dto.getForm() != null ? dto.getForm() : "";
        if (company.isBlank() && form.isBlank()) {
            return dto.getAccessionNumber() != null ? dto.getAccessionNumber() : "SEC Filing";
        }
        if (company.isBlank()) {
            return form + " - " + (dto.getFilingDate() != null ? dto.getFilingDate() : "");
        }
        if (form.isBlank()) {
            return company + " - " + (dto.getFilingDate() != null ? dto.getFilingDate() : "");
        }
        return company + " - " + form + " (" + (dto.getFilingDate() != null ? dto.getFilingDate() : "") + ")";
    }

    private String buildSummary(SecEdgarItemDto dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getCompanyName() != null && !dto.getCompanyName().isBlank()) {
            sb.append(dto.getCompanyName());
        }
        if (dto.getForm() != null && !dto.getForm().isBlank()) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(dto.getForm());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private LocalDateTime parseFilingDate(String filingDate) {
        if (filingDate == null || filingDate.length() < 10) {
            return LocalDateTime.now();
        }
        try {
            LocalDate date = LocalDate.parse(filingDate.substring(0, 10), SEC_DATE);
            return date.atStartOfDay();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
