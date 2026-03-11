package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.news.NewsSentimentScorer;
import com.investment.news.dto.NewsItemPageResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.investment.news.dto.NewsItemDto;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewsItemService")
class NewsItemServiceTest {

        @Mock
        private NewsItemRepository newsItemRepository;
        @Mock
        private NewsSentimentScorer newsSentimentScorer;

        @InjectMocks
        private NewsItemService newsItemService;

        @Test
        @DisplayName("getNewsItems 필터 없이 조회 시 페이징 결과를 반환한다")
        void getNewsItems_withNoFilters_returnsPagedResult() {
                Page<NewsItem> emptyPage = new PageImpl<>(Collections.emptyList(),
                                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "collectedAt")), 0);
                when(newsItemRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                                .thenReturn(emptyPage);

                NewsItemPageResponseDto result = newsItemService.getNewsItems(null, null, null, null, null, null, null, 0, 20);

                assertNotNull(result);
                assertNotNull(result.getContent());
                assertTrue(result.getContent().isEmpty());
                assertNotNull(result.getPage());
                assertEquals(0, result.getPage().getNumber());
                assertEquals(20, result.getPage().getSize());
                assertEquals(0, result.getPage().getTotalElements());
        }

        @Test
        @DisplayName("getNewsItems market·source·symbol 빈 문자열이면 null로 전체 조회한다")
        void getNewsItems_blankStrings_normalizedToNullForAll() {
                Page<NewsItem> emptyPage = new PageImpl<>(Collections.emptyList(),
                                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "collectedAt")), 0);
                when(newsItemRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                                .thenReturn(emptyPage);

                NewsItemPageResponseDto result = newsItemService.getNewsItems("", "  ", "\t", null, null, null, null, 0, 20);

                assertNotNull(result);
                verify(newsItemRepository).findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("getNewsItems 시장 필터 적용 시 repository에 전달한다")
        void getNewsItems_withMarketFilter_callsRepositoryWithMarket() {
                Page<NewsItem> page = new PageImpl<>(Collections.emptyList(),
                                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "collectedAt")), 0);
                when(newsItemRepository.findByFilters(eq("KR"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                                .thenReturn(page);

                NewsItemPageResponseDto result = newsItemService.getNewsItems("KR", null, null, null, null, null, null, 0, 20);

                assertNotNull(result);
                assertEquals(0, result.getContent().size());
        }

        @Test
        @DisplayName("getNewsItems 항목의 sentimentScore가 null이면 조회 시 스코어를 채워 DTO에 반영한다")
        void getNewsItems_nullSentimentScore_fillsFromScorer() {
                NewsItem item = NewsItem.builder()
                                .source("DART")
                                .market("KR")
                                .itemType("FACT")
                                .title("실적호조")
                                .url("https://example.com/1")
                                .collectedAt(LocalDateTime.now())
                                .build();
                Page<NewsItem> page = new PageImpl<>(List.of(item),
                                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "collectedAt")), 1);
                when(newsItemRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);
                when(newsSentimentScorer.scoreText(eq("실적호조"), any())).thenReturn(BigDecimal.valueOf(2));

                NewsItemPageResponseDto result = newsItemService.getNewsItems(null, null, null, null, null, null, null, 0, 20);

                assertNotNull(result.getContent());
                assertEquals(1, result.getContent().size());
                NewsItemDto dto = result.getContent().get(0);
                assertNotNull(dto.getSentimentScore());
                assertEquals(0, dto.getSentimentScore().compareTo(BigDecimal.valueOf(2)));
        }

        @Test
        @DisplayName("getNewsItems size가 100 초과 시 100으로 제한한다")
        void getNewsItems_sizeOver100_capsAt100() {
                Page<NewsItem> page = new PageImpl<>(Collections.emptyList(),
                                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "collectedAt")), 0);
                when(newsItemRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(page);

                NewsItemPageResponseDto result = newsItemService.getNewsItems(null, null, null, null, null, null, null, 0, 200);

                assertNotNull(result);
                assertEquals(100, result.getPage().getSize());
        }

        @Test
        @DisplayName("saveCollectedItem 중복 시 저장하지 않고 false 반환")
        void saveCollectedItem_duplicate_returnsFalseAndDoesNotSave() {
                NewsItem item = NewsItem.builder()
                                .source("DART")
                                .market("KR")
                                .itemType("FACT")
                                .title("제목")
                                .url("https://dart.fss.or.kr/dsbh001/main.do?rcpNo=123")
                                .collectedAt(LocalDateTime.now())
                                .build();
                when(newsSentimentScorer.scoreText(any(), any())).thenReturn(BigDecimal.ZERO);
                when(newsItemRepository.existsBySourceAndUrl("DART", item.getUrl())).thenReturn(true);

                boolean result = newsItemService.saveCollectedItem(item);

                assertFalse(result);
                verify(newsItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("saveCollectedItem 중복 아닐 때 감정 점수 채워 저장하고 true 반환")
        void saveCollectedItem_notDuplicate_savesAndReturnsTrue() {
                NewsItem item = NewsItem.builder()
                                .source("DART")
                                .market("KR")
                                .itemType("FACT")
                                .title("제목")
                                .url("https://dart.fss.or.kr/dsbh001/main.do?rcpNo=456")
                                .collectedAt(LocalDateTime.now())
                                .build();
                when(newsSentimentScorer.scoreText(any(), any())).thenReturn(BigDecimal.ONE);
                when(newsItemRepository.existsBySourceAndUrl("DART", item.getUrl())).thenReturn(false);

                boolean result = newsItemService.saveCollectedItem(item);

                assertTrue(result);
                verify(newsItemRepository).save(argThat(saved ->
                                saved.getSource().equals("DART") && saved.getSentimentScore() != null
                                                && saved.getSentimentScore().compareTo(BigDecimal.ONE) == 0));
        }

        @Test
        @DisplayName("saveCollectedItem item null 시 false 반환")
        void saveCollectedItem_nullItem_returnsFalse() {
                boolean result = newsItemService.saveCollectedItem(null);
                assertFalse(result);
                verify(newsItemRepository, never()).save(any());
        }
}
