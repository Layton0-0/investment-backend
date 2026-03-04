package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 중기 리밸런싱용 모멘텀 스코어 계산.
 * 1M·3M·6M 수익률 가중합 (0.3, 0.4, 0.3). FactorCalculationService.calculateDualMomentum과 별도.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediumTermMomentumService {

    private static final int DAYS_1M = 21;
    private static final int DAYS_3M = 63;
    private static final int DAYS_6M = 126;
    private static final double WEIGHT_1M = 0.3;
    private static final double WEIGHT_3M = 0.4;
    private static final double WEIGHT_6M = 0.3;
    private static final double TOP_PCT = 0.10;

    private final DailyStockRepository dailyStockRepository;

    /**
     * 기준일·시장에 대해 모멘텀 스코어 순으로 정렬된 종목 목록과 상위 10% 종목 집합 반환 (PIT).
     *
     * @param market   KR or US
     * @param asOfDate 기준일
     * @return 순위 목록(고득점 순), 상위 10% 심볼 집합
     */
    public MomentumRankingResult computeMomentumRanking(String market, LocalDate asOfDate) {
        LocalDate from = asOfDate.minusDays(DAYS_6M);
        List<DailyStock> series = dailyStockRepository.findByMarketAndBasDtBetween(market, from, asOfDate);
        if (series.isEmpty()) {
            log.debug("중기 모멘텀: 데이터 없음, market={}, asOfDate={}", market, asOfDate);
            return new MomentumRankingResult(List.of(), Set.of());
        }

        Map<String, TreeMap<LocalDate, BigDecimal>> closeBySymbol = series.stream()
                .filter(d -> d.getClosePrice() != null && d.getClosePrice().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.groupingBy(DailyStock::getSymbol,
                        Collectors.toMap(DailyStock::getBasDt, DailyStock::getClosePrice, (a, b) -> a, TreeMap::new)));

        List<SymbolScore> scores = new ArrayList<>();
        for (Map.Entry<String, TreeMap<LocalDate, BigDecimal>> e : closeBySymbol.entrySet()) {
            String symbol = e.getKey();
            TreeMap<LocalDate, BigDecimal> closes = e.getValue();
            BigDecimal score = computeScore(closes, asOfDate);
            if (score != null) {
                scores.add(new SymbolScore(symbol, score));
            }
        }

        scores.sort(Comparator.comparing(SymbolScore::getScore).reversed());
        List<String> ordered = scores.stream().map(SymbolScore::getSymbol).collect(Collectors.toList());
        int topN = Math.max(1, (int) Math.ceil(ordered.size() * TOP_PCT));
        Set<String> top10 = new HashSet<>(ordered.subList(0, Math.min(topN, ordered.size())));
        log.debug("중기 모멘텀 랭킹: market={}, asOfDate={}, total={}, top10%={}", market, asOfDate, ordered.size(), top10.size());
        return new MomentumRankingResult(ordered, top10);
    }

    private BigDecimal computeScore(Map<LocalDate, BigDecimal> closes, LocalDate asOfDate) {
        BigDecimal c0 = closes.get(asOfDate);
        if (c0 == null || c0.compareTo(BigDecimal.ZERO) <= 0) return null;
        LocalDate d1m = asOfDate.minusDays(DAYS_1M);
        LocalDate d3m = asOfDate.minusDays(DAYS_3M);
        LocalDate d6m = asOfDate.minusDays(DAYS_6M);
        BigDecimal c1m = findClosestClose(closes, d1m);
        BigDecimal c3m = findClosestClose(closes, d3m);
        BigDecimal c6m = findClosestClose(closes, d6m);
        if (c1m == null || c3m == null || c6m == null) return null;
        double r1m = (c0.doubleValue() - c1m.doubleValue()) / c1m.doubleValue();
        double r3m = (c0.doubleValue() - c3m.doubleValue()) / c3m.doubleValue();
        double r6m = (c0.doubleValue() - c6m.doubleValue()) / c6m.doubleValue();
        double score = WEIGHT_1M * r1m + WEIGHT_3M * r3m + WEIGHT_6M * r6m;
        return BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal findClosestClose(Map<LocalDate, BigDecimal> closes, LocalDate target) {
        if (closes.containsKey(target)) return closes.get(target);
        for (int i = 0; i <= 5; i++) {
            LocalDate d = target.minusDays(i);
            if (closes.containsKey(d)) return closes.get(d);
        }
        return null;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class MomentumRankingResult {
        private final List<String> orderedSymbols;
        private final Set<String> top10PercentSymbols;
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class SymbolScore {
        private final String symbol;
        private final BigDecimal score;
    }
}
