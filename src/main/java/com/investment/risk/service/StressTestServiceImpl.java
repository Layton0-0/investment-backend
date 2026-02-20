package com.investment.risk.service;

import com.investment.risk.dto.StressScenario;
import com.investment.risk.dto.StressTestResult;
import com.investment.risk.dto.StressTestResult.AssetClassImpact;
import com.investment.risk.dto.StressTestResult.RiskGrade;
import com.investment.risk.dto.StressTestResult.SymbolImpact;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 스트레스 테스트 서비스 구현체.
 * Historical Stress Test를 통해 포트폴리오의 위기 대응력을 분석합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StressTestServiceImpl implements StressTestService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CRITICAL_THRESHOLD = new BigDecimal("-40");
    private static final BigDecimal HIGH_THRESHOLD = new BigDecimal("-25");
    private static final BigDecimal MEDIUM_THRESHOLD = new BigDecimal("-10");

    private final Map<String, StressScenario> scenarioRegistry = initializeScenarios();

    private Map<String, StressScenario> initializeScenarios() {
        Map<String, StressScenario> scenarios = new LinkedHashMap<>();
        scenarios.put("FINANCIAL_CRISIS_2008", StressScenario.financialCrisis2008());
        scenarios.put("COVID_CRASH_2020", StressScenario.covidCrash2020());
        scenarios.put("RATE_HIKE_2022", StressScenario.rateHike2022());
        scenarios.put("BLACK_MONDAY_1987", StressScenario.blackMonday1987());
        return scenarios;
    }

    @Override
    public StressTestResult runStressTest(Map<String, PortfolioPosition> portfolio, String scenarioCode) {
        StressScenario scenario = scenarioRegistry.get(scenarioCode.toUpperCase());
        if (scenario == null) {
            return StressTestResult.error(scenarioCode, "Unknown scenario code: " + scenarioCode);
        }
        return runStressTest(portfolio, scenario);
    }

    @Override
    public StressTestResult runStressTest(Map<String, PortfolioPosition> portfolio, StressScenario scenario) {
        long startTime = System.currentTimeMillis();

        if (portfolio == null || portfolio.isEmpty()) {
            return StressTestResult.insufficientData(scenario.getCode());
        }

        try {
            BigDecimal totalValueBefore = calculateTotalValue(portfolio);
            if (totalValueBefore.compareTo(BigDecimal.ZERO) <= 0) {
                return StressTestResult.insufficientData(scenario.getCode());
            }

            List<SymbolImpact> symbolImpacts = new ArrayList<>();
            Map<String, AssetClassImpact> assetClassImpacts = new HashMap<>();
            BigDecimal totalLossAmount = BigDecimal.ZERO;

            Map<String, BigDecimal> assetClassTotals = new HashMap<>();
            Map<String, BigDecimal> assetClassLosses = new HashMap<>();

            for (Map.Entry<String, PortfolioPosition> entry : portfolio.entrySet()) {
                PortfolioPosition position = entry.getValue();
                String assetClass = normalizeAssetClass(position.assetClass());

                BigDecimal shock = determineShock(scenario, position.symbol(), assetClass);
                BigDecimal lossAmount = position.value().multiply(shock.abs());
                BigDecimal valueAfter = position.value().add(position.value().multiply(shock));
                BigDecimal lossPct = shock.multiply(HUNDRED);
                BigDecimal weight = position.value().divide(totalValueBefore, 6, RoundingMode.HALF_UP);
                BigDecimal contribution = lossPct.multiply(weight).setScale(4, RoundingMode.HALF_UP);

                symbolImpacts.add(SymbolImpact.builder()
                        .symbol(position.symbol())
                        .market(position.market())
                        .assetClass(assetClass)
                        .currentValue(position.value())
                        .expectedLossPct(lossPct)
                        .expectedLossAmount(lossAmount)
                        .valueAfterStress(valueAfter)
                        .weight(weight.multiply(HUNDRED))
                        .contributionToPortfolioLoss(contribution)
                        .build());

                assetClassTotals.merge(assetClass, position.value(), BigDecimal::add);
                assetClassLosses.merge(assetClass, lossAmount, BigDecimal::add);
                totalLossAmount = totalLossAmount.add(lossAmount);
            }

            for (String assetClass : assetClassTotals.keySet()) {
                BigDecimal total = assetClassTotals.get(assetClass);
                BigDecimal loss = assetClassLosses.getOrDefault(assetClass, BigDecimal.ZERO);
                BigDecimal shock = getAssetClassShock(scenario, assetClass);
                BigDecimal weight = total.divide(totalValueBefore, 6, RoundingMode.HALF_UP);

                assetClassImpacts.put(assetClass, AssetClassImpact.builder()
                        .assetClass(assetClass)
                        .totalValue(total)
                        .appliedShock(shock.multiply(HUNDRED))
                        .expectedLossAmount(loss)
                        .weight(weight.multiply(HUNDRED))
                        .build());
            }

            BigDecimal portfolioLossPct = totalValueBefore.compareTo(BigDecimal.ZERO) > 0
                    ? totalLossAmount.negate().divide(totalValueBefore, 6, RoundingMode.HALF_UP).multiply(HUNDRED)
                    : BigDecimal.ZERO;
            BigDecimal portfolioValueAfter = totalValueBefore.subtract(totalLossAmount);

            symbolImpacts.sort((a, b) -> b.getContributionToPortfolioLoss().abs()
                    .compareTo(a.getContributionToPortfolioLoss().abs()));

            long elapsed = System.currentTimeMillis() - startTime;

            return StressTestResult.builder()
                    .scenarioCode(scenario.getCode())
                    .scenarioName(scenario.getName())
                    .scenarioDescription(scenario.getDescription())
                    .portfolioLossPct(portfolioLossPct)
                    .portfolioLossAmount(totalLossAmount)
                    .portfolioValueBefore(totalValueBefore)
                    .portfolioValueAfter(portfolioValueAfter)
                    .symbolImpacts(symbolImpacts)
                    .assetClassImpacts(assetClassImpacts)
                    .appliedVixLevel(scenario.getVixLevel())
                    .elapsedTimeMs(elapsed)
                    .calculatedAt(Instant.now())
                    .valid(true)
                    .riskGrade(determineRiskGrade(portfolioLossPct))
                    .build();

        } catch (Exception e) {
            log.error("Stress test failed for scenario {}", scenario.getCode(), e);
            return StressTestResult.error(scenario.getCode(), "Calculation error: " + e.getMessage());
        }
    }

    @Override
    public List<StressTestResult> runAllStressTests(Map<String, PortfolioPosition> portfolio) {
        return scenarioRegistry.values().stream()
                .map(scenario -> runStressTest(portfolio, scenario))
                .collect(Collectors.toList());
    }

    @Override
    public StressScenario createCustomScenario(
            String code,
            String name,
            String description,
            Map<String, BigDecimal> assetClassShocks) {

        StressScenario custom = StressScenario.builder()
                .code(code.toUpperCase())
                .name(name)
                .description(description)
                .assetClassShocks(assetClassShocks)
                .correlationIncrease(new BigDecimal("0.20"))
                .vixLevel(new BigDecimal("40"))
                .durationDays(30)
                .build();

        scenarioRegistry.put(code.toUpperCase(), custom);
        log.info("Custom stress scenario created: {}", code);

        return custom;
    }

    @Override
    public List<String> getSupportedScenarioCodes() {
        return new ArrayList<>(scenarioRegistry.keySet());
    }

    @Override
    public StressScenario getScenario(String scenarioCode) {
        return scenarioRegistry.get(scenarioCode.toUpperCase());
    }

    private BigDecimal calculateTotalValue(Map<String, PortfolioPosition> portfolio) {
        return portfolio.values().stream()
                .map(PortfolioPosition::value)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal determineShock(StressScenario scenario, String symbol, String assetClass) {
        if (scenario.getSymbolShocks() != null && scenario.getSymbolShocks().containsKey(symbol)) {
            return scenario.getSymbolShocks().get(symbol);
        }
        return getAssetClassShock(scenario, assetClass);
    }

    private BigDecimal getAssetClassShock(StressScenario scenario, String assetClass) {
        if (scenario.getAssetClassShocks() == null) {
            return new BigDecimal("-0.20");
        }
        return scenario.getAssetClassShocks().getOrDefault(assetClass,
                scenario.getAssetClassShocks().getOrDefault("EQUITY", new BigDecimal("-0.20")));
    }

    private String normalizeAssetClass(String assetClass) {
        if (assetClass == null || assetClass.isBlank()) {
            return "EQUITY";
        }
        String upper = assetClass.toUpperCase().trim();
        return switch (upper) {
            case "STOCK", "STOCKS", "주식" -> "EQUITY";
            case "BONDS", "채권" -> "BOND";
            case "COMMODITIES", "원자재" -> "COMMODITY";
            case "REAL ESTATE", "부동산" -> "REAL_ESTATE";
            case "GROWTH", "성장주" -> "GROWTH_EQUITY";
            default -> upper;
        };
    }

    private RiskGrade determineRiskGrade(BigDecimal lossPct) {
        if (lossPct.compareTo(CRITICAL_THRESHOLD) <= 0) {
            return RiskGrade.CRITICAL;
        } else if (lossPct.compareTo(HIGH_THRESHOLD) <= 0) {
            return RiskGrade.HIGH;
        } else if (lossPct.compareTo(MEDIUM_THRESHOLD) <= 0) {
            return RiskGrade.MEDIUM;
        } else {
            return RiskGrade.LOW;
        }
    }
}
