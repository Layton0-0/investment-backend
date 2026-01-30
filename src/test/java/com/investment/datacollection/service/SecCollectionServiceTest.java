package com.investment.datacollection.service;

import com.investment.datacollection.client.SecEdgarApiClient;
import com.investment.datacollection.dto.SecEdgarItemDto;
import com.investment.domain.entity.NewsItem;
import com.investment.news.service.NewsItemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecCollectionService")
class SecCollectionServiceTest {

    @Mock
    private SecEdgarApiClient secEdgarApiClient;

    @Mock
    private NewsItemService newsItemService;

    @InjectMocks
    private SecCollectionService secCollectionService;

    @Test
    @DisplayName("collectAndSave 시 source=SEC_EDGAR, market=US로 NewsItem을 저장한다")
    void collectAndSave_savesWithSourceSecEdgarAndMarketUs() {
        SecEdgarItemDto dto = SecEdgarItemDto.builder()
                .accessionNumber("0000320193-26-000005")
                .form("8-K")
                .filingDate("2026-01-29")
                .primaryDocument("0000320193-26-000005.htm")
                .cik("0000320193")
                .companyName("Apple Inc.")
                .build();
        when(secEdgarApiClient.fetchRecentFilings(anyInt())).thenReturn(List.of(dto));
        when(newsItemService.saveCollectedItem(any(NewsItem.class))).thenReturn(true);

        int saved = secCollectionService.collectAndSave();

        assertEquals(1, saved);
        ArgumentCaptor<NewsItem> captor = ArgumentCaptor.forClass(NewsItem.class);
        verify(newsItemService, times(1)).saveCollectedItem(captor.capture());
        NewsItem item = captor.getValue();
        assertEquals("SEC_EDGAR", item.getSource());
        assertEquals("US", item.getMarket());
        assertEquals("FACT", item.getItemType());
        assertNotNull(item.getTitle());
        assertTrue(item.getTitle().contains("Apple Inc."));
        assertTrue(item.getUrl().contains("sec.gov"));
    }

    @Test
    @DisplayName("수집 결과가 없으면 저장 호출이 없다")
    void collectAndSave_whenNoItems_doesNotCallSave() {
        when(secEdgarApiClient.fetchRecentFilings(anyInt())).thenReturn(List.of());

        int saved = secCollectionService.collectAndSave();

        assertEquals(0, saved);
        verify(newsItemService, never()).saveCollectedItem(any(NewsItem.class));
    }
}
