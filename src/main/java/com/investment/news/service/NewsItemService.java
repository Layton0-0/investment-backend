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
         * @param market 시장 (KR, US). null이면 전체
         * @param source 원천 코드. null이면 전체
         * @param symbol 종목 코드. null이면 전체
         * @param from   수집 시작일 (포함). null이면 제한 없음
         * @param to     수집 종료일 (포함). null이면 제한 없음
         */
        @Transactional(readOnly = true)
        public NewsItemPageResponseDto getNewsItems(String market, String source, String symbol,
                        LocalDate from, LocalDate to, int page, int size) {
                LocalDateTime fromAt = from != null ? from.atStartOfDay() : null;
                LocalDateTime toAt = to != null ? to.atTime(LocalTime.MAX) : null;

                Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                                Sort.by(Sort.Direction.DESC, "collectedAt"));

                Page<NewsItem> result = newsItemRepository.findByFilters(market, source, symbol, fromAt, toAt,
                                pageable);
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

        private NewsItemDto toDto(NewsItem n) {
                return NewsItemDto.builder()
                                .id(n.getId())
                                .source(n.getSource())
                                .market(n.getMarket())
                                .itemType(n.getItemType())
                                .title(n.getTitle())
                                .summary(n.getSummary())
                                .url(n.getUrl())
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
