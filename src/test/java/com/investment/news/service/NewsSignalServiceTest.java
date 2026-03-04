package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.news.NewsSentimentScorer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewsSignalService")
class NewsSignalServiceTest {

    @Mock
    private NewsItemRepository newsItemRepository;

    @Mock
    private NewsSentimentScorer newsSentimentScorer;

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

    @Test
    @DisplayName("getSymbolScoresWithSignalNews 센티멘트 점수 합산 반영")
    void getSymbolScoresWithSignalNews_includesSentiment() {
        ReflectionTestUtils.setField(newsSignalService, "signalLookbackDays", 7);
        ReflectionTestUtils.setField(newsSignalService, "signalWeight", new BigDecimal("1.0"));
        ReflectionTestUtils.setField(newsSignalService, "sentimentWeight", new BigDecimal("0.1"));
        NewsItem signalItem = NewsItem.builder()
                .source("DART").market("KR").itemType("FACT")
                .title("t").url("u").collectedAt(LocalDateTime.now())
                .eventType("DART_SIGNAL:무상증자")
                .symbol("005930")
                .build();
        when(newsItemRepository.findSignalRelevantSince(eq("KR"), any(LocalDateTime.class)))
                .thenReturn(List.of(signalItem));
        NewsItem newsItem = NewsItem.builder()
                .source("DART").market("KR").itemType("FACT")
                .title("실적호조").summary("매출증가")
                .url("u2").collectedAt(LocalDateTime.now().minusHours(12))
                .symbol("005930")
                .build();
        when(newsItemRepository.findByMarketAndCollectedAtSince(eq("KR"), any(LocalDateTime.class)))
                .thenReturn(List.of(newsItem));
        when(newsSentimentScorer.scoreText(anyString(), anyString())).thenReturn(new BigDecimal("1.5"));

        Map<String, BigDecimal> scores = newsSignalService.getSymbolScoresWithSignalNews("KR", LocalDate.now());

        assertThat(scores).containsKey("005930");
        assertThat(scores.get("005930")).isEqualByComparingTo(new BigDecimal("1.15")); // 1.0 + 1.5 * 0.1
    }

    @Test
    @DisplayName("calculateNewsSentimentBySymbol 빈 뉴스면 빈 맵")
    void calculateNewsSentimentBySymbol_emptyNews_returnsEmptyMap() {
        when(newsItemRepository.findByMarketAndCollectedAtSince(eq("KR"), any(LocalDateTime.class)))
                .thenReturn(List.of());
        Map<String, BigDecimal> result = newsSignalService.calculateNewsSentimentBySymbol("KR", LocalDate.now());
        assertThat(result).isEmpty();
    }
}
