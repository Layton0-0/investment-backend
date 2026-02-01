package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
                when(newsItemRepository.existsBySourceAndUrl("DART", item.getUrl())).thenReturn(true);

                boolean result = newsItemService.saveCollectedItem(item);

                assertFalse(result);
                verify(newsItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("saveCollectedItem 중복 아닐 때 저장하고 true 반환")
        void saveCollectedItem_notDuplicate_savesAndReturnsTrue() {
                NewsItem item = NewsItem.builder()
                                .source("DART")
                                .market("KR")
                                .itemType("FACT")
                                .title("제목")
                                .url("https://dart.fss.or.kr/dsbh001/main.do?rcpNo=456")
                                .collectedAt(LocalDateTime.now())
                                .build();
                when(newsItemRepository.existsBySourceAndUrl("DART", item.getUrl())).thenReturn(false);

                boolean result = newsItemService.saveCollectedItem(item);

                assertTrue(result);
                verify(newsItemRepository).save(item);
        }

        @Test
        @DisplayName("saveCollectedItem item null 시 false 반환")
        void saveCollectedItem_nullItem_returnsFalse() {
                boolean result = newsItemService.saveCollectedItem(null);
                assertFalse(result);
                verify(newsItemRepository, never()).save(any());
        }
}
