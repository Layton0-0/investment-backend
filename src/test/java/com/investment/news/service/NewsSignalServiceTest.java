package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewsSignalService")
class NewsSignalServiceTest {

    @Mock
    private NewsItemRepository newsItemRepository;

    @InjectMocks
    private NewsSignalService newsSignalService;

    @Test
    @DisplayName("getSymbolsWithSignalNews 빈 목록이면 빈 집합 반환")
    void getSymbolsWithSignalNews_empty_returnsEmptySet() {
        ReflectionTestUtils.setField(newsSignalService, "signalLookbackDays", 7);
        when(newsItemRepository.findSignalRelevantSince(eq("KR"), any(LocalDateTime.class)))
                .thenReturn(List.of());

        var result = newsSignalService.getSymbolsWithSignalNews("KR", LocalDate.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSymbolsWithSignalNews DART_SIGNAL·8K 공시 symbol 수집")
    void getSymbolsWithSignalNews_collectsSymbolsFromSignalRelevant() {
        ReflectionTestUtils.setField(newsSignalService, "signalLookbackDays", 7);
        NewsItem a = NewsItem.builder()
                .source("DART").market("KR").itemType("FACT")
                .title("t").url("u").collectedAt(LocalDateTime.now())
                .eventType("DART_SIGNAL:무상증자")
                .symbol("005930")
                .build();
        NewsItem b = NewsItem.builder()
                .source("SEC_EDGAR").market("US").itemType("FACT")
                .title("t").url("u").collectedAt(LocalDateTime.now())
                .eventType("8K")
                .symbol(null)
                .build();
        when(newsItemRepository.findSignalRelevantSince(eq("KR"), any(LocalDateTime.class)))
                .thenReturn(List.of(a));

        var result = newsSignalService.getSymbolsWithSignalNews("KR", LocalDate.now());

        assertThat(result).containsExactly("005930");
    }

    @Test
    @DisplayName("getSymbolsWithSignalNews null market이면 빈 집합")
    void getSymbolsWithSignalNews_nullMarket_returnsEmpty() {
        var result = newsSignalService.getSymbolsWithSignalNews(null, LocalDate.now());
        assertThat(result).isEmpty();
    }
}
