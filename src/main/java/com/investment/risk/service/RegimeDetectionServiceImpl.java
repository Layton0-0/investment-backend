package com.investment.risk.service;

import com.investment.config.CacheConfig;
import com.investment.config.RiskProperties;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.strategy.engine.MacroIndicatorProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * VIX + SPY 이평선 기반 시장 레짐 탐지.
 * BULL = SPY 50일선 &gt; 200일선 AND VIX &lt; 20.
 * BEAR = SPY 50일선 &lt; 200일선 AND VIX &gt; 30.
 * 그 외 NEUTRAL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegimeDetectionServiceImpl implements RegimeDetectionService {

    private static final String SPY = "SPY";
    private static final String MARKET_US = "US";
    private static final int MA50_DAYS = 50;
    private static final int MA200_DAYS = 200;
    private static final BigDecimal VIX_BULL_MAX = new BigDecimal("20");
    private static final BigDecimal VIX_BEAR_MIN = new BigDecimal("30");

    private final DailyStockRepository dailyStockRepository;
    private final MacroIndicatorProvider macroIndicatorProvider;
    private final RiskProperties riskProperties;

    @Override
    @Cacheable(value = CacheConfig.CACHE_REGIME, key = "#asOfDate != null ? #asOfDate.toString() : 'default'", unless = "#result == null")
    public RegimeResult getCurrentRegime(LocalDate asOfDate) {
        if (!riskProperties.isRegimeDetectionEnabled()) {
            return new RegimeResult(MacroDashboardResponse.MarketRegime.NEUTRAL, 0.0, null, null, null);
        }
        LocalDate base = asOfDate != null ? asOfDate : LocalDate.now();
        LocalDate from = base.minusDays(MA200_DAYS + 30);

        List<DailyStock> spyHistory = dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                SPY, MARKET_US, from, base);
        if (spyHistory.size() < MA200_DAYS) {
            log.debug("레짐 탐지: SPY 일봉 부족 (필요 {}일, 실제 {}일)", MA200_DAYS, spyHistory.size());
            return fallbackFromVixOnly(base);
        }

        List<DailyStock> ordered = spyHistory.stream()
                .filter(d -> d.getClosePrice() != null && d.getClosePrice().compareTo(BigDecimal.ZERO) > 0)
                .sorted((a, b) -> b.getBasDt().compareTo(a.getBasDt()))
                .limit(MA200_DAYS)
                .toList();
        if (ordered.size() < MA200_DAYS) {
            return fallbackFromVixOnly(base);
        }

        BigDecimal ma50 = ordered.stream()
                .limit(MA50_DAYS)
                .map(DailyStock::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(MA50_DAYS), 4, RoundingMode.HALF_UP);
        BigDecimal ma200 = ordered.stream()
                .map(DailyStock::getClosePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(ordered.size()), 4, RoundingMode.HALF_UP);

        BigDecimal vix = getVix(base);

        MacroDashboardResponse.MarketRegime regime;
        double confidence;
        if (ma50.compareTo(ma200) > 0 && (vix == null || vix.compareTo(VIX_BULL_MAX) < 0)) {
            regime = MacroDashboardResponse.MarketRegime.BULL;
            confidence = vix != null ? 0.85 : 0.7;
        } else if (ma50.compareTo(ma200) < 0 && vix != null && vix.compareTo(VIX_BEAR_MIN) > 0) {
            regime = MacroDashboardResponse.MarketRegime.BEAR;
            confidence = 0.85;
        } else {
            regime = MacroDashboardResponse.MarketRegime.NEUTRAL;
            confidence = 0.65;
        }

        log.debug("레짐 탐지: SPY ma50={}, ma200={}, VIX={} -> {}", ma50, ma200, vix, regime);
        return new RegimeResult(regime, confidence, ma50, ma200, vix);
    }

    private RegimeResult fallbackFromVixOnly(LocalDate base) {
        BigDecimal vix = getVix(base);
        if (vix != null && vix.compareTo(VIX_BEAR_MIN) > 0) {
            return new RegimeResult(MacroDashboardResponse.MarketRegime.BEAR, 0.6, null, null, vix);
        }
        if (vix != null && vix.compareTo(VIX_BULL_MAX) < 0) {
            return new RegimeResult(MacroDashboardResponse.MarketRegime.BULL, 0.5, null, null, vix);
        }
        return new RegimeResult(MacroDashboardResponse.MarketRegime.NEUTRAL, 0.0, null, null, vix);
    }

    private BigDecimal getVix(LocalDate base) {
        return macroIndicatorProvider.getCurrentIndicators()
                .map(ind -> ind.getVix())
                .filter(v -> v != null)
                .orElse(null);
    }
}
