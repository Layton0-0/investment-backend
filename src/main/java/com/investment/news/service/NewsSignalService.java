package com.investment.news.service;

import com.investment.domain.entity.NewsItem;
import com.investment.domain.repository.NewsItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 공시·뉴스 기반 시그널 연동 (13-news-collection-design).
 * DART 키워드 매칭(DART_SIGNAL:), SEC 8-K 등 시그널 반영 대상 공시를 조회해
 * 포지션 사이징·유니버스 완화 등에서 사용.
 */
@Service
@RequiredArgsConstructor
public class NewsSignalService {

    private final NewsItemRepository newsItemRepository;

    /** 시그널 반영 조회 기간(일). 최근 N일 이내 공시만 대상 */
    @Value("${investment.news.signal-lookback-days:7}")
    private int signalLookbackDays = 7;

    /** 시그널 공시 종목 정렬 시 가중치 (포지션 사이징 시 시그널 종목 우선도). 1.0 = 기본 */
    @Value("${investment.news.signal-weight:1.0}")
    private BigDecimal signalWeight = BigDecimal.ONE;

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
     * 시그널 있음 = signalWeight, 없음 = 0.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getSymbolScoresWithSignalNews(String market, LocalDate basDt) {
        Set<String> symbols = getSymbolsWithSignalNews(market, basDt);
        Map<String, BigDecimal> scores = new HashMap<>();
        for (String s : symbols) {
            scores.put(s, signalWeight != null ? signalWeight : BigDecimal.ONE);
        }
        return scores;
    }
}
