package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.news.NewsSentimentScorer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공시·뉴스 기반 시그널 연동 (13-news-collection-design).
 * DART 키워드 매칭(DART_SIGNAL:), SEC 8-K 등 시그널 반영 대상 공시를 조회하고,
 * 경량 감성사전 기반 센티멘트 점수를 시그널 가중치에 반영.
 */
@Service
@RequiredArgsConstructor
public class NewsSignalService {

    private final NewsItemRepository newsItemRepository;
    private final NewsSentimentScorer newsSentimentScorer;

    /** 시그널 반영 조회 기간(일). 최근 N일 이내 공시만 대상 */
    @Value("${investment.news.signal-lookback-days:7}")
    private int signalLookbackDays = 7;

    /** 시그널 공시 종목 정렬 시 가중치 (포지션 사이징 시 시그널 종목 우선도). 1.0 = 기본 */
    @Value("${investment.news.signal-weight:1.0}")
    private BigDecimal signalWeight = BigDecimal.ONE;

    /** 센티멘트 점수를 시그널 점수에 반영하는 비율 (종목별 평균 센티멘트 * 이 값이 가산). 기본 0.1 */
    @Value("${investment.news.sentiment-weight:0.1}")
    private BigDecimal sentimentWeight = new BigDecimal("0.1");

    /**
     * 해당 시장에서 기준일(basDt) 기준 최근 signalLookbackDays 일 이내 시그널 반영 대상 공시에 등장한 종목 코드 집합.
     * KR: DART_SIGNAL 공시의 symbol. US: 8-K는 symbol 미저장으로 빈 집합 가능.
     */
    @Transactional(readOnly = true)
    public Set<String> getSymbolsWithSignalNews(String market, LocalDate basDt) {
        if (market == null || market.isBlank()) {
            return Set.of();
        }
        LocalDate sinceDate = basDt.minusDays(signalLookbackDays);
        LocalDateTime since = sinceDate.atStartOfDay();
        List<NewsItem> items = newsItemRepository.findSignalRelevantSince(market, since);
        return items.stream()
                .map(NewsItem::getSymbol)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toSet());
    }

    /**
     * 시그널 공시가 있는 종목별 점수(가중치). 포지션 사이징 정렬 시 사용.
     * 시그널 있음 = signalWeight + (종목별 최근 24시간 뉴스 센티멘트 평균 * sentimentWeight), 없음 = 0.
     * 뉴스 없거나 키워드 없음 시 센티멘트 0으로 처리.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSymbolScoresWithSignalNews(String market, LocalDate basDt) {
        Set<String> symbols = getSymbolsWithSignalNews(market, basDt);
        Map<String, BigDecimal> scores = new HashMap<>();
        BigDecimal base = signalWeight != null ? signalWeight : BigDecimal.ONE;
        Map<String, BigDecimal> sentimentBySymbol = calculateNewsSentimentBySymbol(market, basDt);
        for (String s : symbols) {
            BigDecimal sentiment = sentimentBySymbol.getOrDefault(s, BigDecimal.ZERO);
            BigDecimal add = sentiment.multiply(sentimentWeight != null ? sentimentWeight : new BigDecimal("0.1"));
            scores.put(s, base.add(add).setScale(4, RoundingMode.HALF_UP));
        }
        return scores;
    }

    /**
     * 최근 24시간 뉴스의 title/summary에 대해 경량 감성사전으로 종목별 평균 센티멘트 계산.
     * 뉴스 없거나 키워드 없음 = 0.
     */
    public Map<String, BigDecimal> calculateNewsSentimentBySymbol(String market, LocalDate basDt) {
        if (market == null || market.isBlank()) {
            return Map.of();
        }
        LocalDateTime since = basDt.atStartOfDay().minusHours(24);
        List<NewsItem> items = newsItemRepository.findByMarketAndCollectedAtSince(market, since);
        if (items.isEmpty()) {
            return Map.of();
        }
        Map<String, List<BigDecimal>> bySymbol = new HashMap<>();
        for (NewsItem item : items) {
            String sym = item.getSymbol() != null && !item.getSymbol().isBlank() ? item.getSymbol() : null;
            if (sym == null) continue;
            BigDecimal score = newsSentimentScorer.scoreText(item.getTitle(), item.getSummary());
            bySymbol.computeIfAbsent(sym, k -> new java.util.ArrayList<>()).add(score);
        }
        Map<String, BigDecimal> result = new HashMap<>();
        int scale = 4;
        for (Map.Entry<String, List<BigDecimal>> e : bySymbol.entrySet()) {
            List<BigDecimal> list = e.getValue();
            if (list.isEmpty()) continue;
            BigDecimal sum = list.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put(e.getKey(), sum.divide(BigDecimal.valueOf(list.size()), scale, RoundingMode.HALF_UP));
        }
        return result;
    }
}
