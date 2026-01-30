package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.news.dto.NewsItemDto;
import com.investment.news.dto.NewsItemPageResponseDto;
import lombok.RequiredArgsConstructor;
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
