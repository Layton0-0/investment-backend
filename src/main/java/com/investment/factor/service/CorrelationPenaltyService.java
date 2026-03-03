package com.investment.factor.service;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.factor.dto.PositionRecommendationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 포트폴리오 수준 상관관계 리스크 통제.
 * 권장 포지션 목록에 대해 종목 간 상관계수가 높을 때 비중을 스케일 다운하여
 * 섹터/자산 동시 폭락 시 포트폴리오 리스크를 완화한다.
 *
 * @see PositionSizingService
 * @see investment-backend/docs/02-architecture/00-strategy-registry.md §2.5.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorrelationPenaltyService {

    private static final int LOOKBACK_DAYS = 60;
    private static final int MIN_DAYS_FOR_CORRELATION = 20;

    private final DailyStockRepository dailyStockRepository;

    @Value("${investment.factor.correlation-penalty-enabled:false}")
    private boolean correlationPenaltyEnabled = false;

    /** 상관계수 임계값 초과 시 패널티 적용 (기본 0.7) */
    @Value("${investment.factor.correlation-threshold:0.7}")
    private double correlationThreshold = 0.7;

    /** 고상관 시 적용할 비중 스케일 팩터 (0~1, 기본 0.8) */
    @Value("${investment.factor.correlation-penalty-scale:0.8}")
    private double correlationPenaltyScale = 0.8;

    /**
     * 권장 포지션 목록에 상관관계 패널티 적용.
     * 고상관 쌍이 존재하면 전체 비중에 스케일 팩터를 곱해 반환한다.
     *
     * @param recommendations Half-Kelly·변동성 역가중·리스크 캡 적용 후 목록
     * @param totalCapital     총 투자 가능 자산
     * @param market           시장 (KR, US)
     * @param basDt            기준일 (수익률 구간 종료일)
     * @return 패널티 적용된 목록 (enabled=false 또는 데이터 부족 시 입력 그대로)
     */
    public List<PositionRecommendationDto> applyPenalty(
            List<PositionRecommendationDto> recommendations,
            BigDecimal totalCapital,
            String market,
            LocalDate basDt) {
        if (!correlationPenaltyEnabled || recommendations == null || recommendations.isEmpty()) {
            return recommendations;
        }
        if (totalCapital == null || totalCapital.compareTo(BigDecimal.ZERO) <= 0) {
            return recommendations;
        }
        List<String> symbols = recommendations.stream()
                .map(PositionRecommendationDto::getSymbol)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (symbols.size() < 2) {
            return recommendations;
        }

        LocalDate fromDt = basDt.minusDays(LOOKBACK_DAYS);
        List<DailyStock> rows = dailyStockRepository.findByMarketAndSymbolInAndBasDtBetweenOrderByBasDtAsc(
                market, symbols, fromDt, basDt);
        Map<String, List<BigDecimal>> returnsBySymbol = buildReturnSeriesBySymbol(rows, symbols);
        List<String> validSymbols = returnsBySymbol.entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_DAYS_FOR_CORRELATION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (validSymbols.size() < 2) {
            log.debug("CorrelationPenalty: insufficient return data, market={}, basDt={}, symbols={}",
                    market, basDt, symbols.size());
            return recommendations;
        }

        boolean hasHighCorrelation = hasAnyPairAboveThreshold(returnsBySymbol, validSymbols, correlationThreshold);
        if (!hasHighCorrelation) {
            return recommendations;
        }

        BigDecimal scale = BigDecimal.valueOf(correlationPenaltyScale);
        log.info("CorrelationPenalty: high correlation pair detected, applying scale {} to {} recommendations, market={}",
                scale, recommendations.size(), market);
        return recommendations.stream()
                .map(rec -> scaleRecommendation(rec, scale))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean hasAnyPairAboveThreshold(Map<String, List<BigDecimal>> returnsBySymbol,
                                             List<String> validSymbols,
                                             double threshold) {
        int n = validSymbols.size();
        int minLen = validSymbols.stream()
                .mapToInt(s -> returnsBySymbol.get(s).size())
                .min()
                .orElse(0);
        if (minLen < MIN_DAYS_FOR_CORRELATION) {
            return false;
        }
        double[][] data = new double[n][minLen];
        for (int i = 0; i < n; i++) {
            List<BigDecimal> ret = returnsBySymbol.get(validSymbols.get(i));
            int start = ret.size() - minLen;
            for (int t = 0; t < minLen; t++) {
                data[i][t] = ret.get(start + t).doubleValue();
            }
        }
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double r = pearson(data[i], data[j], minLen);
                if (r >= threshold) {
                    log.debug("CorrelationPenalty: pair {} vs {} correlation={}", validSymbols.get(i), validSymbols.get(j), r);
                    return true;
                }
            }
        }
        return false;
    }

    private static double pearson(double[] x, double[] y, int len) {
        double sumX = 0, sumY = 0, sumX2 = 0, sumY2 = 0, sumXY = 0;
        for (int i = 0; i < len; i++) {
            sumX += x[i];
            sumY += y[i];
            sumX2 += x[i] * x[i];
            sumY2 += y[i] * y[i];
            sumXY += x[i] * y[i];
        }
        double n = len;
        double num = n * sumXY - sumX * sumY;
        double den = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY));
        if (den == 0) {
            return 0.0;
        }
        return num / den;
    }

    private Map<String, List<BigDecimal>> buildReturnSeriesBySymbol(List<DailyStock> rows, List<String> symbols) {
        Map<String, Map<LocalDate, BigDecimal>> closeBySymbolByDate = new LinkedHashMap<>();
        for (String symbol : symbols) {
            closeBySymbolByDate.put(symbol, new TreeMap<>());
        }
        for (DailyStock d : rows) {
            if (d.getClosePrice() == null || !closeBySymbolByDate.containsKey(d.getSymbol())) {
                continue;
            }
            closeBySymbolByDate.get(d.getSymbol()).put(d.getBasDt(), d.getClosePrice());
        }
        Set<LocalDate> allDates = new TreeSet<>();
        for (Map<LocalDate, BigDecimal> m : closeBySymbolByDate.values()) {
            allDates.addAll(m.keySet());
        }
        List<LocalDate> commonDates = allDates.stream()
                .filter(dt -> closeBySymbolByDate.values().stream().allMatch(m -> m.containsKey(dt)))
                .sorted()
                .collect(Collectors.toList());
        if (commonDates.size() < 2) {
            return symbols.stream()
                    .collect(Collectors.toMap(s -> s, s -> List.<BigDecimal>of(), (a, b) -> a, LinkedHashMap::new));
        }
        Map<String, List<BigDecimal>> out = new LinkedHashMap<>();
        for (String symbol : symbols) {
            Map<LocalDate, BigDecimal> closes = closeBySymbolByDate.get(symbol);
            List<BigDecimal> returns = new ArrayList<>();
            for (int i = 1; i < commonDates.size(); i++) {
                LocalDate prevDt = commonDates.get(i - 1);
                LocalDate currDt = commonDates.get(i);
                BigDecimal prev = closes.get(prevDt);
                BigDecimal curr = closes.get(currDt);
                if (prev == null || curr == null || prev.compareTo(BigDecimal.ZERO) == 0) {
                    continue;
                }
                returns.add(curr.subtract(prev).divide(prev, 6, RoundingMode.HALF_UP));
            }
            out.put(symbol, returns);
        }
        return out;
    }

    private PositionRecommendationDto scaleRecommendation(PositionRecommendationDto rec, BigDecimal scale) {
        if (rec.getEntryPrice() == null || rec.getEntryPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return rec;
        }
        BigDecimal newAmt = rec.getRecommendedAmt().multiply(scale).setScale(0, RoundingMode.DOWN);
        long qty = newAmt.divide(rec.getEntryPrice(), 0, RoundingMode.DOWN).longValue();
        if (qty <= 0) {
            return null;
        }
        BigDecimal actualAmt = rec.getEntryPrice().multiply(BigDecimal.valueOf(qty));
        return PositionRecommendationDto.builder()
                .basDt(rec.getBasDt())
                .symbol(rec.getSymbol())
                .market(rec.getMarket())
                .recommendedAmt(actualAmt)
                .recommendedQty(qty)
                .method(rec.getMethod() != null ? rec.getMethod() + "+CORR_PENALTY" : "CORR_PENALTY")
                .entryPrice(rec.getEntryPrice())
                .stopLoss(rec.getStopLoss())
                .build();
    }
}
