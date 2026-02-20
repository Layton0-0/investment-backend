package com.investment.factor.zoo;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.entity.SignalScore;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.factor.service.UniverseFilterService;
import com.investment.factor.zoo.FactorDefinition.FactorCategory;
import com.investment.factor.zoo.FactorTestResult.FactorGrade;
import com.investment.factor.zoo.FactorTestResult.QuantileReturn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Factor Zoo 서비스 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactorZooServiceImpl implements FactorZooService {

    private static final int DEFAULT_QUANTILES = 5;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final DailyStockRepository dailyStockRepository;
    private final SignalScoreRepository signalScoreRepository;
    private final UniverseFilterService universeFilterService;

    private final Map<String, FactorDefinition> factorRegistry = initializeFactors();

    private Map<String, FactorDefinition> initializeFactors() {
        Map<String, FactorDefinition> factors = new LinkedHashMap<>();

        factors.put("PBR", FactorDefinition.pbr());
        factors.put("PER", FactorDefinition.per());
        factors.put("EV_EBITDA", FactorDefinition.evEbitda());

        factors.put("MOMENTUM_3M", FactorDefinition.momentum3m());
        factors.put("MOMENTUM_6M", FactorDefinition.momentum6m());
        factors.put("MOMENTUM_12M", FactorDefinition.momentum12m());

        factors.put("ROE", FactorDefinition.roe());
        factors.put("OPERATING_MARGIN", FactorDefinition.operatingMargin());
        factors.put("DEBT_RATIO", FactorDefinition.debtRatio());

        factors.put("MARKET_CAP", FactorDefinition.marketCap());

        factors.put("VOLATILITY", FactorDefinition.volatility());
        factors.put("BETA", FactorDefinition.beta());

        factors.put("DISPARITY", FactorDefinition.disparity());
        factors.put("VOLATILITY_BREAKOUT", FactorDefinition.volatilityBreakout());
        factors.put("SMART_MONEY_INTENSITY", FactorDefinition.smartMoneyIntensity());

        return factors;
    }

    @Override
    public FactorTestResult testFactor(String factorCode, String market, LocalDate startDate, LocalDate endDate) {
        FactorDefinition definition = factorRegistry.get(factorCode.toUpperCase());
        if (definition == null) {
            return FactorTestResult.error(factorCode, "Unknown factor code: " + factorCode);
        }

        try {
            List<String> universe = universeFilterService.getSymbols(endDate, market);
            if (universe.isEmpty()) {
                universe = getUniverseFromDailyStock(market, startDate, endDate);
            }
            if (universe.size() < 30) {
                return FactorTestResult.insufficientData(factorCode);
            }

            List<BigDecimal> icValues = new ArrayList<>();
            Map<LocalDate, BigDecimal> icTimeSeries = new LinkedHashMap<>();
            List<QuantileReturn> quantileReturns = new ArrayList<>();

            LocalDate testDate = startDate;
            while (!testDate.isAfter(endDate.minusDays(21))) {
                BigDecimal ic = computeIC(factorCode, market, universe, testDate, 21);
                if (ic != null) {
                    if (definition.getDirection() == FactorDefinition.FactorDirection.LOWER_BETTER) {
                        ic = ic.negate();
                    }
                    icValues.add(ic);
                    icTimeSeries.put(testDate, ic);
                }
                testDate = testDate.plusDays(21);
            }

            if (icValues.isEmpty()) {
                return FactorTestResult.insufficientData(factorCode);
            }

            BigDecimal avgIC = icValues.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(icValues.size()), 6, RoundingMode.HALF_UP);

            BigDecimal icStdDev = computeStdDev(icValues);
            BigDecimal icTStat = icStdDev.compareTo(BigDecimal.ZERO) > 0
                    ? avgIC.divide(icStdDev, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(Math.sqrt(icValues.size())))
                    : BigDecimal.ZERO;

            BigDecimal ir = icStdDev.compareTo(BigDecimal.ZERO) > 0
                    ? avgIC.divide(icStdDev, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            quantileReturns = computeQuantileReturns(factorCode, market, universe, endDate.minusDays(63), endDate);
            BigDecimal longShortSpread = BigDecimal.ZERO;
            if (quantileReturns.size() >= 2) {
                BigDecimal topReturn = quantileReturns.get(0).getAvgReturnPct();
                BigDecimal bottomReturn = quantileReturns.get(quantileReturns.size() - 1).getAvgReturnPct();
                if (definition.getDirection() == FactorDefinition.FactorDirection.LOWER_BETTER) {
                    longShortSpread = bottomReturn.subtract(topReturn);
                } else {
                    longShortSpread = topReturn.subtract(bottomReturn);
                }
            }

            BigDecimal turnover = computeTurnover(factorCode, market, universe, startDate, endDate);
            BigDecimal icDecay = computeICDecay(icValues);

            FactorGrade grade = determineGrade(avgIC.abs(), ir.abs());

            return FactorTestResult.builder()
                    .factorCode(factorCode)
                    .factorName(definition.getName())
                    .category(definition.getCategory())
                    .direction(definition.getDirection())
                    .testStartDate(startDate)
                    .testEndDate(endDate)
                    .market(market)
                    .universeSize(universe.size())
                    .informationCoefficient(avgIC)
                    .icTStat(icTStat)
                    .icStdDev(icStdDev)
                    .informationRatio(ir)
                    .turnoverPct(turnover)
                    .icDecay(icDecay)
                    .quantileReturns(quantileReturns)
                    .longShortSpreadPct(longShortSpread)
                    .icTimeSeries(icTimeSeries)
                    .valid(true)
                    .grade(grade)
                    .calculatedAt(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Factor test failed for {}", factorCode, e);
            return FactorTestResult.error(factorCode, "Calculation error: " + e.getMessage());
        }
    }

    @Override
    public List<FactorTestResult> rankFactors(String market, LocalDate startDate, LocalDate endDate) {
        return factorRegistry.keySet().stream()
                .map(code -> testFactor(code, market, startDate, endDate))
                .filter(FactorTestResult::isValid)
                .sorted((a, b) -> b.getInformationCoefficient().abs().compareTo(a.getInformationCoefficient().abs()))
                .collect(Collectors.toList());
    }

    @Override
    public CombinedFactorScore getCombinedScore(String symbol, String market, LocalDate basDt,
                                                 Map<String, BigDecimal> factorWeights) {
        Map<String, BigDecimal> factorScores = new HashMap<>();
        BigDecimal combinedScore = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Map.Entry<String, BigDecimal> entry : factorWeights.entrySet()) {
            String factorCode = entry.getKey();
            BigDecimal weight = entry.getValue();

            BigDecimal score = getFactorScore(symbol, market, basDt, factorCode);
            if (score != null) {
                factorScores.put(factorCode, score);
                combinedScore = combinedScore.add(score.multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            combinedScore = combinedScore.divide(totalWeight, 4, RoundingMode.HALF_UP);
        }

        return new CombinedFactorScore(symbol, market, basDt, combinedScore, factorScores, factorWeights, 0);
    }

    @Override
    public List<CombinedFactorScore> rankStocksByFactors(String market, LocalDate basDt,
                                                          Map<String, BigDecimal> factorWeights, int topN) {
        List<String> universe = universeFilterService.getSymbols(basDt, market);
        if (universe.isEmpty()) {
            universe = getUniverseFromDailyStock(market, basDt.minusDays(30), basDt);
        }

        List<CombinedFactorScore> scores = universe.stream()
                .map(symbol -> getCombinedScore(symbol, market, basDt, factorWeights))
                .sorted((a, b) -> b.combinedScore().compareTo(a.combinedScore()))
                .limit(topN)
                .collect(Collectors.toList());

        List<CombinedFactorScore> ranked = new ArrayList<>();
        for (int i = 0; i < scores.size(); i++) {
            CombinedFactorScore s = scores.get(i);
            ranked.add(new CombinedFactorScore(s.symbol(), s.market(), s.basDt(),
                    s.combinedScore(), s.factorScores(), s.factorWeights(), i + 1));
        }

        return ranked;
    }

    @Override
    public FactorDefinition getFactorDefinition(String factorCode) {
        return factorRegistry.get(factorCode.toUpperCase());
    }

    @Override
    public List<FactorDefinition> getAllFactorDefinitions() {
        return new ArrayList<>(factorRegistry.values());
    }

    @Override
    public List<FactorDefinition> getFactorsByCategory(FactorCategory category) {
        return factorRegistry.values().stream()
                .filter(f -> f.getCategory() == category)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getSupportedFactorCodes() {
        return new ArrayList<>(factorRegistry.keySet());
    }

    private BigDecimal computeIC(String factorCode, String market, List<String> universe,
                                  LocalDate basDt, int forwardDays) {
        List<BigDecimal> factorValues = new ArrayList<>();
        List<BigDecimal> returns = new ArrayList<>();

        for (String symbol : universe) {
            BigDecimal factorValue = getFactorScore(symbol, market, basDt, factorCode);
            BigDecimal forwardReturn = computeForwardReturn(symbol, market, basDt, forwardDays);

            if (factorValue != null && forwardReturn != null) {
                factorValues.add(factorValue);
                returns.add(forwardReturn);
            }
        }

        if (factorValues.size() < 20) {
            return null;
        }

        return computeSpearmanCorrelation(factorValues, returns);
    }

    private BigDecimal getFactorScore(String symbol, String market, LocalDate basDt, String factorCode) {
        String factorType = mapFactorCodeToSignalType(factorCode);
        if (factorType != null) {
            return signalScoreRepository.findByBasDtAndSymbolAndFactorTypeAndMarket(basDt, symbol, factorType, market)
                    .map(SignalScore::getScore)
                    .orElse(null);
        }

        return computeFactorFromDailyStock(symbol, market, basDt, factorCode);
    }

    private String mapFactorCodeToSignalType(String factorCode) {
        return switch (factorCode.toUpperCase()) {
            case "DISPARITY" -> "DISPARITY";
            case "VOLATILITY_BREAKOUT" -> "VOLATILITY_BREAKOUT";
            case "SMART_MONEY_INTENSITY" -> "SMART_MONEY_INTENSITY";
            case "DUAL_MOMENTUM", "MOMENTUM_3M", "MOMENTUM_6M", "MOMENTUM_12M" -> "DUAL_MOMENTUM";
            default -> null;
        };
    }

    private BigDecimal computeFactorFromDailyStock(String symbol, String market, LocalDate basDt, String factorCode) {
        List<DailyStock> history = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                symbol, market, basDt.minusDays(252), basDt);

        if (history.isEmpty()) {
            return null;
        }

        return switch (factorCode.toUpperCase()) {
            case "MOMENTUM_3M" -> computeMomentum(history, basDt, 63);
            case "MOMENTUM_6M" -> computeMomentum(history, basDt, 126);
            case "MOMENTUM_12M" -> computeMomentum(history, basDt, 252);
            case "VOLATILITY" -> computeVolatility(history, 20);
            case "PBR", "PER", "ROE", "EV_EBITDA", "OPERATING_MARGIN", "DEBT_RATIO", "BETA", "MARKET_CAP" ->
                    getMockFundamentalValue(factorCode);
            default -> null;
        };
    }

    private BigDecimal computeMomentum(List<DailyStock> history, LocalDate basDt, int days) {
        if (history.size() < days) {
            return null;
        }

        DailyStock current = history.stream()
                .filter(d -> !d.getBasDt().isAfter(basDt))
                .max(Comparator.comparing(DailyStock::getBasDt))
                .orElse(null);

        LocalDate pastDate = basDt.minusDays(days);
        DailyStock past = history.stream()
                .filter(d -> !d.getBasDt().isAfter(pastDate))
                .max(Comparator.comparing(DailyStock::getBasDt))
                .orElse(null);

        if (current == null || past == null ||
                current.getClosePrice() == null || past.getClosePrice() == null ||
                past.getClosePrice().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return current.getClosePrice().subtract(past.getClosePrice())
                .divide(past.getClosePrice(), 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    private BigDecimal computeVolatility(List<DailyStock> history, int window) {
        if (history.size() < window + 1) {
            return null;
        }

        List<DailyStock> recent = history.subList(Math.max(0, history.size() - window - 1), history.size());
        List<BigDecimal> returns = new ArrayList<>();

        for (int i = 1; i < recent.size(); i++) {
            DailyStock prev = recent.get(i - 1);
            DailyStock curr = recent.get(i);
            if (prev.getClosePrice() != null && curr.getClosePrice() != null &&
                    prev.getClosePrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal ret = curr.getClosePrice().subtract(prev.getClosePrice())
                        .divide(prev.getClosePrice(), 6, RoundingMode.HALF_UP);
                returns.add(ret);
            }
        }

        if (returns.isEmpty()) {
            return null;
        }

        return computeStdDev(returns).multiply(BigDecimal.valueOf(Math.sqrt(252)))
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getMockFundamentalValue(String factorCode) {
        Random random = new Random();
        return switch (factorCode.toUpperCase()) {
            case "PBR" -> BigDecimal.valueOf(0.5 + random.nextDouble() * 3.0);
            case "PER" -> BigDecimal.valueOf(5 + random.nextDouble() * 30);
            case "ROE" -> BigDecimal.valueOf(random.nextDouble() * 30);
            case "EV_EBITDA" -> BigDecimal.valueOf(3 + random.nextDouble() * 15);
            case "OPERATING_MARGIN" -> BigDecimal.valueOf(random.nextDouble() * 25);
            case "DEBT_RATIO" -> BigDecimal.valueOf(random.nextDouble() * 200);
            case "BETA" -> BigDecimal.valueOf(0.5 + random.nextDouble() * 1.5);
            case "MARKET_CAP" -> BigDecimal.valueOf(1000 + random.nextDouble() * 100000);
            default -> null;
        };
    }

    private BigDecimal computeForwardReturn(String symbol, String market, LocalDate basDt, int forwardDays) {
        List<DailyStock> future = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                symbol, market, basDt, basDt.plusDays(forwardDays + 10));

        if (future.size() < 2) {
            return null;
        }

        DailyStock start = future.get(0);
        DailyStock end = future.stream()
                .filter(d -> !d.getBasDt().isBefore(basDt.plusDays(forwardDays - 5)))
                .findFirst()
                .orElse(future.get(future.size() - 1));

        if (start.getClosePrice() == null || end.getClosePrice() == null ||
                start.getClosePrice().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return end.getClosePrice().subtract(start.getClosePrice())
                .divide(start.getClosePrice(), 6, RoundingMode.HALF_UP)
                .multiply(HUNDRED);
    }

    private BigDecimal computeSpearmanCorrelation(List<BigDecimal> x, List<BigDecimal> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            return null;
        }

        int n = x.size();
        int[] rankX = computeRanks(x);
        int[] rankY = computeRanks(y);

        double sumD2 = 0;
        for (int i = 0; i < n; i++) {
            double d = rankX[i] - rankY[i];
            sumD2 += d * d;
        }

        double rho = 1 - (6 * sumD2) / (n * (n * n - 1.0));
        return BigDecimal.valueOf(rho).setScale(4, RoundingMode.HALF_UP);
    }

    private int[] computeRanks(List<BigDecimal> values) {
        int n = values.size();
        Integer[] indices = new Integer[n];
        for (int i = 0; i < n; i++) {
            indices[i] = i;
        }

        Arrays.sort(indices, (a, b) -> values.get(a).compareTo(values.get(b)));

        int[] ranks = new int[n];
        for (int rank = 0; rank < n; rank++) {
            ranks[indices[rank]] = rank + 1;
        }

        return ranks;
    }

    private BigDecimal computeStdDev(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal mean = values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);

        BigDecimal variance = values.stream()
                .map(v -> v.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP);

        return BigDecimal.valueOf(Math.sqrt(variance.doubleValue())).setScale(4, RoundingMode.HALF_UP);
    }

    private List<QuantileReturn> computeQuantileReturns(String factorCode, String market,
                                                         List<String> universe, LocalDate startDate, LocalDate endDate) {
        Map<String, BigDecimal> factorScores = new HashMap<>();
        Map<String, BigDecimal> returns = new HashMap<>();

        for (String symbol : universe) {
            BigDecimal score = getFactorScore(symbol, market, startDate, factorCode);
            BigDecimal ret = computeForwardReturn(symbol, market, startDate,
                    (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate));

            if (score != null && ret != null) {
                factorScores.put(symbol, score);
                returns.put(symbol, ret);
            }
        }

        if (factorScores.size() < DEFAULT_QUANTILES) {
            return Collections.emptyList();
        }

        List<String> sorted = factorScores.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        int quantileSize = sorted.size() / DEFAULT_QUANTILES;
        List<QuantileReturn> result = new ArrayList<>();

        for (int q = 0; q < DEFAULT_QUANTILES; q++) {
            int start = q * quantileSize;
            int end = (q == DEFAULT_QUANTILES - 1) ? sorted.size() : (q + 1) * quantileSize;

            List<String> quantileSymbols = sorted.subList(start, end);
            BigDecimal avgReturn = quantileSymbols.stream()
                    .map(returns::get)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(quantileSymbols.size()), 4, RoundingMode.HALF_UP);

            result.add(QuantileReturn.builder()
                    .quantile(q + 1)
                    .label("Q" + (q + 1))
                    .stockCount(quantileSymbols.size())
                    .avgReturnPct(avgReturn)
                    .cumulativeReturnPct(avgReturn)
                    .build());
        }

        return result;
    }

    private BigDecimal computeTurnover(String factorCode, String market, List<String> universe,
                                        LocalDate startDate, LocalDate endDate) {
        return new BigDecimal("25.00");
    }

    private BigDecimal computeICDecay(List<BigDecimal> icValues) {
        if (icValues.size() < 3) {
            return BigDecimal.ZERO;
        }

        int half = icValues.size() / 2;
        BigDecimal firstHalfAvg = icValues.subList(0, half).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(half), 4, RoundingMode.HALF_UP);

        BigDecimal secondHalfAvg = icValues.subList(half, icValues.size()).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(icValues.size() - half), 4, RoundingMode.HALF_UP);

        if (firstHalfAvg.abs().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return secondHalfAvg.subtract(firstHalfAvg)
                .divide(firstHalfAvg.abs(), 4, RoundingMode.HALF_UP);
    }

    private FactorGrade determineGrade(BigDecimal ic, BigDecimal ir) {
        if (ic.compareTo(new BigDecimal("0.05")) >= 0 && ir.compareTo(new BigDecimal("0.5")) >= 0) {
            return FactorGrade.A;
        } else if (ic.compareTo(new BigDecimal("0.03")) >= 0 && ir.compareTo(new BigDecimal("0.3")) >= 0) {
            return FactorGrade.B;
        } else if (ic.compareTo(new BigDecimal("0.02")) >= 0 && ir.compareTo(new BigDecimal("0.2")) >= 0) {
            return FactorGrade.C;
        } else if (ic.compareTo(new BigDecimal("0.01")) >= 0) {
            return FactorGrade.D;
        } else {
            return FactorGrade.F;
        }
    }

    private List<String> getUniverseFromDailyStock(String market, LocalDate startDate, LocalDate endDate) {
        List<DailyStock> stocks = dailyStockRepository.findByMarketAndBasDtBetween(market, startDate, endDate);
        return stocks.stream()
                .map(DailyStock::getSymbol)
                .distinct()
                .collect(Collectors.toList());
    }
}
