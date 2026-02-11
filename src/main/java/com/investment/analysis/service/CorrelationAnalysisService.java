package com.investment.analysis.service;

import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.analysis.dto.CorrelationAnalysisResponseDto;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
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
 * 포트폴리오(또는 종목 목록) 내 종목 간 일봉 수익률 상관계수 행렬 계산.
 * TB_DAILY_STOCK 기반. 최소 2종목·최소 일수 미만이면 빈 행렬 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CorrelationAnalysisService {

    private static final int DEFAULT_LOOKBACK_DAYS = 60;
    private static final int MIN_DAYS_FOR_CORRELATION = 20;

    private final DailyStockRepository dailyStockRepository;
    private final AccountService accountService;

    @Value("${investment.analysis.correlation-lookback-days:60}")
    private int correlationLookbackDays = DEFAULT_LOOKBACK_DAYS;

    /**
     * 계좌 보유 종목 기준 상관관계 분석 (인증 사용자).
     *
     * @param userId    사용자 ID
     * @param accountNo 계좌번호
     * @param from      기간 시작 (null이면 to 기준 lookback)
     * @param to        기간 종료 (null이면 오늘)
     * @return 상관계수 행렬 DTO. 보유 종목 2개 미만 또는 데이터 부족 시 빈 행렬
     */
    public CorrelationAnalysisResponseDto getCorrelationByAccount(String userId, String accountNo,
                                                                  LocalDate from, LocalDate to) {
        if (userId == null || accountNo == null || accountNo.isBlank()) {
            return emptyResponse(null, from, to);
        }
        BalanceAndPositionsDto balanceAndPositions = accountService.getBalanceAndPositionsWithUserId(userId, accountNo);
        if (balanceAndPositions == null || balanceAndPositions.getPositions() == null
                || balanceAndPositions.getPositions().isEmpty()) {
            return emptyResponse(null, from, to);
        }
        List<String> symbols = balanceAndPositions.getPositions().stream()
                .map(p -> p.getSymbol())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        String market = balanceAndPositions.getPositions().stream()
                .map(p -> p.getMarket())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("US");
        return computeCorrelation(market, symbols, resolveFromTo(from, to));
    }

    /**
     * 종목 목록·시장 기준 상관관계 분석.
     *
     * @param symbols 종목 코드 목록 (쉼표 구분 문자열 또는 리스트)
     * @param market  시장 (KR/US)
     * @param from    기간 시작 (null이면 to 기준 lookback)
     * @param to      기간 종료 (null이면 오늘)
     * @return 상관계수 행렬 DTO
     */
    public CorrelationAnalysisResponseDto getCorrelationBySymbols(List<String> symbols, String market,
                                                                  LocalDate from, LocalDate to) {
        if (symbols == null || symbols.isEmpty()) {
            return emptyResponse(Optional.ofNullable(market).orElse("US"), from, to);
        }
        List<String> list = symbols.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty())
                .distinct().collect(Collectors.toList());
        String marketNorm = Optional.ofNullable(market).orElse("US").toUpperCase();
        return computeCorrelation(marketNorm, list, resolveFromTo(from, to));
    }

    private LocalDate[] resolveFromTo(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(correlationLookbackDays);
        if (start.isAfter(end)) {
            start = end.minusDays(correlationLookbackDays);
        }
        return new LocalDate[]{start, end};
    }

    private CorrelationAnalysisResponseDto emptyResponse(String market, LocalDate from, LocalDate to) {
        return CorrelationAnalysisResponseDto.builder()
                .market(Optional.ofNullable(market).orElse("US"))
                .symbols(List.of())
                .fromDate(from)
                .toDate(to)
                .matrix(List.of())
                .build();
    }

    private CorrelationAnalysisResponseDto computeCorrelation(String market, List<String> symbols,
                                                             LocalDate[] fromTo) {
        LocalDate fromDt = fromTo[0];
        LocalDate toDt = fromTo[1];
        if (symbols.size() < 2) {
            return CorrelationAnalysisResponseDto.builder()
                    .market(market)
                    .symbols(List.of())
                    .fromDate(fromDt)
                    .toDate(toDt)
                    .matrix(List.of())
                    .build();
        }
        List<DailyStock> rows = dailyStockRepository.findByMarketAndSymbolInAndBasDtBetweenOrderByBasDtAsc(
                market, symbols, fromDt, toDt);
        Map<String, List<BigDecimal>> returnsBySymbol = buildReturnSeriesBySymbol(rows, symbols);
        List<String> validSymbols = returnsBySymbol.entrySet().stream()
                .filter(e -> e.getValue().size() >= MIN_DAYS_FOR_CORRELATION)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (validSymbols.size() < 2) {
            log.debug("Correlation: insufficient data for market={}, symbols={}, from={}, to={}",
                    market, symbols, fromDt, toDt);
            return CorrelationAnalysisResponseDto.builder()
                    .market(market)
                    .symbols(symbols)
                    .fromDate(fromDt)
                    .toDate(toDt)
                    .matrix(List.of())
                    .build();
        }
        List<List<Double>> matrix = buildCorrelationMatrix(returnsBySymbol, validSymbols);
        return CorrelationAnalysisResponseDto.builder()
                .market(market)
                .symbols(validSymbols)
                .fromDate(fromDt)
                .toDate(toDt)
                .matrix(matrix)
                .build();
    }

    /** Align return series by common dates so correlation is computed on same-day returns. */
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
            return closeBySymbolByDate.keySet().stream()
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

    /**
     * Align return series by length (take minimum length so all have same size for pairwise correlation).
     * Then compute Pearson correlation matrix for validSymbols.
     */
    private List<List<Double>> buildCorrelationMatrix(Map<String, List<BigDecimal>> returnsBySymbol,
                                                      List<String> validSymbols) {
        int n = validSymbols.size();
        int minLen = validSymbols.stream()
                .mapToInt(s -> returnsBySymbol.get(s).size())
                .min()
                .orElse(0);
        if (minLen < MIN_DAYS_FOR_CORRELATION) {
            return List.of();
        }
        double[][] data = new double[n][minLen];
        for (int i = 0; i < n; i++) {
            List<BigDecimal> ret = returnsBySymbol.get(validSymbols.get(i));
            int start = ret.size() - minLen;
            for (int t = 0; t < minLen; t++) {
                data[i][t] = ret.get(start + t).doubleValue();
            }
        }
        List<List<Double>> matrix = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            List<Double> row = new ArrayList<>(n);
            for (int j = 0; j < n; j++) {
                double r = (i == j) ? 1.0 : pearson(data[i], data[j], minLen);
                row.add(Math.round(r * 1_000_000.0) / 1_000_000.0);
            }
            matrix.add(row);
        }
        return matrix;
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
}
