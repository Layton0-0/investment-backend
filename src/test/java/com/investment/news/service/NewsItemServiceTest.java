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
                when(newsItemRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                                .thenReturn(emptyPage);

                NewsItemPageResponseDto result = newsItemService.getNewsItems(null, null, null, null, null, 0, 20);

                assertNotNull(result);
                assertNotNull(result.getContent());
                assertTrue(result.getContent().isEmpty());
                assertNotNull(result.getPage());
                assertEquals(0, result.getPage().getNumber());
                assertEquals(20, result.getPage().getSize());
                assertEquals(0, result.getPage().getTotalElements());
        }

        @Test
        @DisplayName("getNewsItems 시장 필터 적용 시 repository에 전달한다")
        void getNewsItems_withMarketFilter_callsRepositoryWithMarket() {
                Page<NewsItem> page = new PageImpl<>(Collections.emptyList(),
                                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "collectedAt")), 0);
                when(newsItemRepository.findByFilters(eq("KR"), isNull(), isNull(), isNull(), isNull(), any()))
                                .thenReturn(page);

                NewsItemPageResponseDto result = newsItemService.getNewsItems("KR", null, null, null, null, 0, 20);

                assertNotNull(result);
                assertEquals(0, result.getContent().size());
        }

        @Test
        @DisplayName("getNewsItems size가 100 초과 시 100으로 제한한다")
        void getNewsItems_sizeOver100_capsAt100() {
                Page<NewsItem> page = new PageImpl<>(Collections.emptyList(),
                                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "collectedAt")), 0);
                when(newsItemRepository.findByFilters(any(), any(), any(), any(), any(), any())).thenReturn(page);

                NewsItemPageResponseDto result = newsItemService.getNewsItems(null, null, null, null, null, 0, 200);

                assertNotNull(result);
                assertEquals(100, result.getPage().getSize());
        }
}
