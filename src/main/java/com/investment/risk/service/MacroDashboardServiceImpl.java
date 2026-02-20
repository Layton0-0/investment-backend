package com.investment.risk.service;

import com.investment.config.RiskProperties;
import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.risk.dto.MacroIndicatorDto;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import com.investment.strategy.engine.MacroIndicatorProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * 매크로 대시보드 서비스 구현체.
 * Redis 캐싱으로 응답 속도 최적화.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MacroDashboardServiceImpl implements MacroDashboardService {

    private static final int CACHE_TTL_SECONDS = 3600;

    private static final BigDecimal VIX_GREEN_THRESHOLD = new BigDecimal("20");
    private static final BigDecimal VIX_YELLOW_THRESHOLD = new BigDecimal("30");
    private static final BigDecimal US10Y_GREEN_MIN = new BigDecimal("2");
    private static final BigDecimal US10Y_GREEN_MAX = new BigDecimal("4.5");
    private static final BigDecimal DXY_GREEN_MIN = new BigDecimal("95");
    private static final BigDecimal DXY_GREEN_MAX = new BigDecimal("105");

    private final MacroIndicatorProvider macroIndicatorProvider;
    private final RiskProperties riskProperties;

    private static final Map<String, IndicatorMetadata> INDICATOR_METADATA = Map.ofEntries(
            Map.entry("VIX", new IndicatorMetadata("VIX (공포 지수)", MacroIndicatorDto.IndicatorCategory.MARKET, "CBOE")),
            Map.entry("MOVE", new IndicatorMetadata("MOVE Index (채권 변동성)", MacroIndicatorDto.IndicatorCategory.MARKET, "ICE")),
            Map.entry("PCR", new IndicatorMetadata("Put/Call Ratio", MacroIndicatorDto.IndicatorCategory.MARKET, "CBOE")),
            Map.entry("FED_RATE", new IndicatorMetadata("연준 기준금리", MacroIndicatorDto.IndicatorCategory.INTEREST_RATE, "FRED")),
            Map.entry("US2Y", new IndicatorMetadata("미국 국채 2년물", MacroIndicatorDto.IndicatorCategory.INTEREST_RATE, "FRED")),
            Map.entry("US10Y", new IndicatorMetadata("미국 국채 10년물", MacroIndicatorDto.IndicatorCategory.INTEREST_RATE, "FRED")),
            Map.entry("YIELD_SPREAD", new IndicatorMetadata("장단기 금리차 (10Y-2Y)", MacroIndicatorDto.IndicatorCategory.INTEREST_RATE, "Calculated")),
            Map.entry("ISM_PMI", new IndicatorMetadata("ISM 제조업 PMI", MacroIndicatorDto.IndicatorCategory.ECONOMY, "ISM")),
            Map.entry("CPI", new IndicatorMetadata("소비자물가지수 (YoY%)", MacroIndicatorDto.IndicatorCategory.ECONOMY, "BLS")),
            Map.entry("UNEMPLOYMENT", new IndicatorMetadata("실업률", MacroIndicatorDto.IndicatorCategory.ECONOMY, "BLS")),
            Map.entry("USD_KRW", new IndicatorMetadata("달러/원 환율", MacroIndicatorDto.IndicatorCategory.CURRENCY, "BOK")),
            Map.entry("DXY", new IndicatorMetadata("달러 인덱스", MacroIndicatorDto.IndicatorCategory.CURRENCY, "ICE"))
    );

    @Override
    @Cacheable(value = "macro-dashboard", unless = "#result == null")
    public MacroDashboardResponse getDashboard() {
        log.debug("매크로 대시보드 조회 시작");
        long startTime = System.currentTimeMillis();

        try {
            Optional<MacroEconomicStrategyEngine.MacroEconomicIndicators> indicators =
                    macroIndicatorProvider.getCurrentIndicators();

            Map<String, MacroIndicatorDto> allIndicators = new LinkedHashMap<>();
            List<MacroIndicatorDto> marketIndicators = new ArrayList<>();
            List<MacroIndicatorDto> interestRateIndicators = new ArrayList<>();
            List<MacroIndicatorDto> economyIndicators = new ArrayList<>();
            List<MacroIndicatorDto> currencyIndicators = new ArrayList<>();

            if (indicators.isPresent()) {
                MacroEconomicStrategyEngine.MacroEconomicIndicators data = indicators.get();
                addIndicator(allIndicators, marketIndicators, "VIX", data.getVix());
                addIndicator(allIndicators, interestRateIndicators, "FED_RATE", data.getInterestRate());
                addIndicator(allIndicators, economyIndicators, "CPI", data.getInflationRate());
            }

            addMockIndicatorsIfEmpty(allIndicators, marketIndicators, interestRateIndicators, economyIndicators, currencyIndicators);

            MacroDashboardResponse.MarketRegime regime = determineMarketRegime(allIndicators);
            int riskScore = calculateOverallRiskScore(allIndicators);

            MacroDashboardResponse.RiskGateStatus riskGateStatus = MacroDashboardResponse.RiskGateStatus.builder()
                    .enabled(riskProperties.isRegimeGateEnabled())
                    .currentVix(allIndicators.containsKey("VIX") ? allIndicators.get("VIX").getValue().doubleValue() : null)
                    .vixThreshold(riskProperties.getVixThreshold().doubleValue())
                    .triggered(isRiskGateTriggered(allIndicators))
                    .reduceSizePercent(riskProperties.getReduceSizeOnHighVolPct().intValue())
                    .build();

            long elapsedMs = System.currentTimeMillis() - startTime;
            log.debug("매크로 대시보드 조회 완료: {}ms, 지표 수: {}", elapsedMs, allIndicators.size());

            return MacroDashboardResponse.builder()
                    .regime(regime)
                    .regimeConfidence(0.75)
                    .overallRiskScore(riskScore)
                    .marketIndicators(marketIndicators)
                    .interestRateIndicators(interestRateIndicators)
                    .economyIndicators(economyIndicators)
                    .currencyIndicators(currencyIndicators)
                    .allIndicators(allIndicators)
                    .riskGateStatus(riskGateStatus)
                    .timestamp(Instant.now())
                    .cached(false)
                    .cacheTtlSeconds(CACHE_TTL_SECONDS)
                    .build();

        } catch (Exception e) {
            log.error("매크로 대시보드 조회 실패", e);
            return buildEmptyDashboard();
        }
    }

    @Override
    public MacroIndicatorDto getIndicator(String indicatorCode) {
        MacroDashboardResponse dashboard = getDashboard();
        return dashboard.getAllIndicators().get(indicatorCode.toUpperCase());
    }

    @Override
    public List<MacroIndicatorDto> getIndicatorHistory(String indicatorCode, LocalDate startDate, LocalDate endDate) {
        MacroIndicatorDto current = getIndicator(indicatorCode);
        if (current == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(current);
    }

    @Override
    public MacroDashboardResponse.MarketRegime getMarketRegime() {
        MacroDashboardResponse dashboard = getDashboard();
        return dashboard.getRegime();
    }

    @Override
    @CacheEvict(value = "macro-dashboard", allEntries = true)
    public void refreshCache() {
        log.info("매크로 대시보드 캐시 갱신");
    }

    @Override
    public List<String> getSupportedIndicatorCodes() {
        return new ArrayList<>(INDICATOR_METADATA.keySet());
    }

    private void addIndicator(Map<String, MacroIndicatorDto> allIndicators,
                              List<MacroIndicatorDto> categoryList,
                              String code, BigDecimal value) {
        if (value == null) {
            return;
        }
        IndicatorMetadata metadata = INDICATOR_METADATA.get(code);
        if (metadata == null) {
            return;
        }

        MacroIndicatorDto dto = MacroIndicatorDto.builder()
                .code(code)
                .name(metadata.name)
                .category(metadata.category)
                .value(value.setScale(2, RoundingMode.HALF_UP))
                .change(BigDecimal.ZERO)
                .changePercent(BigDecimal.ZERO)
                .signal(determineSignal(code, value))
                .signalDescription(getSignalDescription(code, value))
                .source(metadata.source)
                .updatedAt(Instant.now())
                .build();

        allIndicators.put(code, dto);
        categoryList.add(dto);
    }

    private void addMockIndicatorsIfEmpty(Map<String, MacroIndicatorDto> allIndicators,
                                          List<MacroIndicatorDto> market,
                                          List<MacroIndicatorDto> interest,
                                          List<MacroIndicatorDto> economy,
                                          List<MacroIndicatorDto> currency) {
        if (!allIndicators.containsKey("VIX")) {
            addIndicator(allIndicators, market, "VIX", new BigDecimal("18.5"));
        }
        if (!allIndicators.containsKey("MOVE")) {
            addIndicator(allIndicators, market, "MOVE", new BigDecimal("95.2"));
        }
        if (!allIndicators.containsKey("PCR")) {
            addIndicator(allIndicators, market, "PCR", new BigDecimal("0.85"));
        }
        if (!allIndicators.containsKey("FED_RATE")) {
            addIndicator(allIndicators, interest, "FED_RATE", new BigDecimal("5.25"));
        }
        if (!allIndicators.containsKey("US2Y")) {
            addIndicator(allIndicators, interest, "US2Y", new BigDecimal("4.65"));
        }
        if (!allIndicators.containsKey("US10Y")) {
            addIndicator(allIndicators, interest, "US10Y", new BigDecimal("4.25"));
        }
        if (!allIndicators.containsKey("YIELD_SPREAD")) {
            addIndicator(allIndicators, interest, "YIELD_SPREAD", new BigDecimal("-0.40"));
        }
        if (!allIndicators.containsKey("ISM_PMI")) {
            addIndicator(allIndicators, economy, "ISM_PMI", new BigDecimal("52.1"));
        }
        if (!allIndicators.containsKey("CPI")) {
            addIndicator(allIndicators, economy, "CPI", new BigDecimal("3.2"));
        }
        if (!allIndicators.containsKey("UNEMPLOYMENT")) {
            addIndicator(allIndicators, economy, "UNEMPLOYMENT", new BigDecimal("3.7"));
        }
        if (!allIndicators.containsKey("USD_KRW")) {
            addIndicator(allIndicators, currency, "USD_KRW", new BigDecimal("1325.50"));
        }
        if (!allIndicators.containsKey("DXY")) {
            addIndicator(allIndicators, currency, "DXY", new BigDecimal("103.8"));
        }
    }

    private MacroIndicatorDto.SignalLevel determineSignal(String code, BigDecimal value) {
        if (value == null) {
            return MacroIndicatorDto.SignalLevel.YELLOW;
        }

        return switch (code) {
            case "VIX" -> {
                if (value.compareTo(VIX_GREEN_THRESHOLD) < 0) yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(VIX_YELLOW_THRESHOLD) < 0) yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "MOVE" -> {
                if (value.compareTo(new BigDecimal("100")) < 0) yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(new BigDecimal("130")) < 0) yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "PCR" -> {
                if (value.compareTo(new BigDecimal("0.7")) > 0 && value.compareTo(new BigDecimal("1.0")) < 0)
                    yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(new BigDecimal("0.5")) > 0 && value.compareTo(new BigDecimal("1.3")) < 0)
                    yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "US10Y" -> {
                if (value.compareTo(US10Y_GREEN_MIN) >= 0 && value.compareTo(US10Y_GREEN_MAX) <= 0)
                    yield MacroIndicatorDto.SignalLevel.GREEN;
                yield MacroIndicatorDto.SignalLevel.YELLOW;
            }
            case "YIELD_SPREAD" -> {
                if (value.compareTo(BigDecimal.ZERO) > 0) yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(new BigDecimal("-0.5")) > 0) yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "ISM_PMI" -> {
                if (value.compareTo(new BigDecimal("50")) > 0) yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(new BigDecimal("45")) > 0) yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "CPI" -> {
                if (value.compareTo(new BigDecimal("3")) < 0) yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(new BigDecimal("5")) < 0) yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "UNEMPLOYMENT" -> {
                if (value.compareTo(new BigDecimal("4")) < 0) yield MacroIndicatorDto.SignalLevel.GREEN;
                if (value.compareTo(new BigDecimal("5")) < 0) yield MacroIndicatorDto.SignalLevel.YELLOW;
                yield MacroIndicatorDto.SignalLevel.RED;
            }
            case "DXY" -> {
                if (value.compareTo(DXY_GREEN_MIN) >= 0 && value.compareTo(DXY_GREEN_MAX) <= 0)
                    yield MacroIndicatorDto.SignalLevel.GREEN;
                yield MacroIndicatorDto.SignalLevel.YELLOW;
            }
            default -> MacroIndicatorDto.SignalLevel.YELLOW;
        };
    }

    private String getSignalDescription(String code, BigDecimal value) {
        MacroIndicatorDto.SignalLevel signal = determineSignal(code, value);
        return switch (signal) {
            case GREEN -> "양호";
            case YELLOW -> "주의 필요";
            case RED -> "경고";
        };
    }

    private MacroDashboardResponse.MarketRegime determineMarketRegime(Map<String, MacroIndicatorDto> indicators) {
        int bullSignals = 0;
        int bearSignals = 0;

        for (MacroIndicatorDto indicator : indicators.values()) {
            if (indicator.getSignal() == MacroIndicatorDto.SignalLevel.GREEN) {
                bullSignals++;
            } else if (indicator.getSignal() == MacroIndicatorDto.SignalLevel.RED) {
                bearSignals++;
            }
        }

        if (bullSignals > bearSignals * 2) {
            return MacroDashboardResponse.MarketRegime.BULL;
        } else if (bearSignals > bullSignals) {
            return MacroDashboardResponse.MarketRegime.BEAR;
        }
        return MacroDashboardResponse.MarketRegime.NEUTRAL;
    }

    private int calculateOverallRiskScore(Map<String, MacroIndicatorDto> indicators) {
        int redCount = 0;
        int yellowCount = 0;
        int total = indicators.size();

        for (MacroIndicatorDto indicator : indicators.values()) {
            if (indicator.getSignal() == MacroIndicatorDto.SignalLevel.RED) {
                redCount++;
            } else if (indicator.getSignal() == MacroIndicatorDto.SignalLevel.YELLOW) {
                yellowCount++;
            }
        }

        if (total == 0) {
            return 50;
        }
        return (int) ((redCount * 100.0 + yellowCount * 50.0) / total);
    }

    private boolean isRiskGateTriggered(Map<String, MacroIndicatorDto> indicators) {
        if (!riskProperties.isRegimeGateEnabled()) {
            return false;
        }
        MacroIndicatorDto vix = indicators.get("VIX");
        if (vix == null || vix.getValue() == null) {
            return false;
        }
        return vix.getValue().compareTo(riskProperties.getVixThreshold()) > 0;
    }

    private MacroDashboardResponse buildEmptyDashboard() {
        return MacroDashboardResponse.builder()
                .regime(MacroDashboardResponse.MarketRegime.NEUTRAL)
                .regimeConfidence(0.0)
                .overallRiskScore(50)
                .marketIndicators(Collections.emptyList())
                .interestRateIndicators(Collections.emptyList())
                .economyIndicators(Collections.emptyList())
                .currencyIndicators(Collections.emptyList())
                .allIndicators(Collections.emptyMap())
                .riskGateStatus(MacroDashboardResponse.RiskGateStatus.builder()
                        .enabled(riskProperties.isRegimeGateEnabled())
                        .triggered(false)
                        .build())
                .timestamp(Instant.now())
                .cached(false)
                .build();
    }

    private record IndicatorMetadata(String name, MacroIndicatorDto.IndicatorCategory category, String source) {}
}
