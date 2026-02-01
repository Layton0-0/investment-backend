package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.news.dto.NewsItemDto;
import com.investment.news.dto.NewsItemPageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 뉴스·공시 항목 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsItemService {

        private final NewsItemRepository newsItemRepository;

        /**
         * 필터·페이징으로 뉴스 목록 조회
         *
         * @param market   시장 (KR, US). null/빈값이면 전체
         * @param source   원천 코드. null/빈값이면 전체
         * @param itemType 유형 (FACT, SPEED, BUZZ). null/빈값이면 전체
         * @param symbol   종목 코드. null/빈값이면 전체
         * @param title    제목 부분 일치. null/빈값이면 제한 없음
         * @param from     수집 시작일 (포함). null이면 제한 없음
         * @param to       수집 종료일 (포함). null이면 제한 없음
         */
        @Transactional(readOnly = true)
        public NewsItemPageResponseDto getNewsItems(String market, String source, String itemType, String symbol,
                        String title, LocalDate from, LocalDate to, int page, int size) {
                market = (market != null && !market.isBlank()) ? market.trim() : null;
                source = (source != null && !source.isBlank()) ? source.trim() : null;
                itemType = (itemType != null && !itemType.isBlank()) ? itemType.trim() : null;
                symbol = (symbol != null && !symbol.isBlank()) ? symbol.trim() : null;
                String titlePattern = (title != null && !title.isBlank()) ? "%" + title.trim() + "%" : null;
                LocalDateTime fromAt = from != null ? from.atStartOfDay() : null;
                LocalDateTime toAt = to != null ? to.atTime(LocalTime.MAX) : null;

                Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                                Sort.by(Sort.Direction.DESC, "collectedAt"));

                Page<NewsItem> result = newsItemRepository.findByFilters(market, source, itemType, symbol,
                                titlePattern, fromAt, toAt, pageable);
                List<NewsItemDto> content = result.getContent().stream()
                                .map(this::toDto)
                                .collect(Collectors.toList());

                return NewsItemPageResponseDto.builder()
                                .content(content)
                                .page(NewsItemPageResponseDto.PageMeta.builder()
                                                .number(result.getNumber())
                                                .size(result.getSize())
                                                .totalElements(result.getTotalElements())
                                                .totalPages(result.getTotalPages())
                                                .build())
                                .build();
        }

        /**
         * 수집 항목 저장 (원천·URL 중복 시 스킵).
         * 로깅 시 source만 노출, URL은 길이만 로그.
         *
         * @param item 수집된 뉴스·공시 항목
         * @return 저장된 경우 true, 중복으로 스킵된 경우 false
         */
        @Transactional
        public boolean saveCollectedItem(NewsItem item) {
                if (item == null || item.getSource() == null || item.getUrl() == null) {
                        log.warn("수집 항목 저장 스킵: source 또는 url 없음");
                        return false;
                }
                String url = item.getUrl();
                if (url.length() > 1000) {
                        url = url.substring(0, 1000);
                        item = NewsItem.builder()
                                        .source(item.getSource())
                                        .market(item.getMarket())
                                        .itemType(item.getItemType())
                                        .title(item.getTitle())
                                        .summary(item.getSummary())
                                        .url(url)
                                        .collectedAt(item.getCollectedAt())
                                        .symbol(item.getSymbol())
                                        .sentimentScore(item.getSentimentScore())
                                        .importanceScore(item.getImportanceScore())
                                        .eventType(item.getEventType())
                                        .build();
                }
                if (newsItemRepository.existsBySourceAndUrl(item.getSource(), item.getUrl())) {
                        log.debug("수집 항목 중복 스킵: source={}, urlLength={}", item.getSource(), item.getUrl().length());
                        return false;
                }
                newsItemRepository.save(item);
                log.info("수집 항목 저장: source={}, market={}, titleLength={}", item.getSource(), item.getMarket(), item.getTitle() != null ? item.getTitle().length() : 0);
                return true;
        }

        /**
         * 표시용 URL 보정. DB에 예전 경로(dsbh001)로 저장된 DART 링크를 현재 정식 경로(dsaf001)로 치환.
         */
        private String fixDisplayUrl(String url) {
                if (url == null || url.isBlank()) {
                        return url;
                }
                if (url.contains("dart.fss.or.kr/dsbh001")) {
                        return url.replace("dsbh001", "dsaf001");
                }
                return url;
        }

        private NewsItemDto toDto(NewsItem n) {
                return NewsItemDto.builder()
                                .id(n.getId())
                                .source(n.getSource())
                                .market(n.getMarket())
                                .itemType(n.getItemType())
                                .title(n.getTitle())
                                .summary(n.getSummary())
                                .url(fixDisplayUrl(n.getUrl()))
                                .collectedAt(n.getCollectedAt())
                                .symbol(n.getSymbol())
                                .sentimentScore(n.getSentimentScore())
                                .importanceScore(n.getImportanceScore())
                                .eventType(n.getEventType())
                                .createdAt(n.getCreatedAt())
                                .updatedAt(n.getUpdatedAt())
                                .build();
        }
}
