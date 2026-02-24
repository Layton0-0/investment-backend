package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 전략 비중 동적 결정 설정.
 * 시장 레짐(MarketRegime)별 단기/중기/장기 목표 비중 및 전역 상·하한.
 */
@Component
@ConfigurationProperties(prefix = "investment.trading.strategy-weights")
@Getter
@Setter
public class StrategyWeightProperties {

    /** 동적 비중 사용 여부. false면 설정 비중 또는 (0.2, 0.4, 0.4) 사용 */
    private boolean enabled = true;

    /** 각 비중의 하한 (0~1). 클리핑용 */
    private BigDecimal minWeight = new BigDecimal("0.05");

    /** 각 비중의 상한 (0~1). 클리핑용 */
    private BigDecimal maxWeight = new BigDecimal("0.55");

    /** 레짐별 목표 비중. 키: MarketRegime.name() (예: HIGH_VOLATILITY) */
    private Map<String, RegimeWeights> regimeWeights = defaultRegimeWeights();

    private static Map<String, RegimeWeights> defaultRegimeWeights() {
        Map<String, RegimeWeights> m = new HashMap<>();
        m.put("HIGH_VOLATILITY", new RegimeWeights("0.10", "0.35", "0.55"));
        m.put("MODERATE_VOLATILITY", new RegimeWeights("0.15", "0.40", "0.45"));
        m.put("RECESSION", new RegimeWeights("0.10", "0.30", "0.60"));
        m.put("GROWTH", new RegimeWeights("0.25", "0.40", "0.35"));
        m.put("NORMAL", new RegimeWeights("0.20", "0.40", "0.40"));
        m.put("LOW_VOLATILITY", new RegimeWeights("0.25", "0.40", "0.35"));
        m.put("HIGH_INTEREST_RATE", new RegimeWeights("0.15", "0.38", "0.47"));
        m.put("HIGH_INFLATION", new RegimeWeights("0.15", "0.38", "0.47"));
        m.put("LOW_INTEREST_RATE", new RegimeWeights("0.20", "0.40", "0.40"));
        m.put("LOW_INFLATION", new RegimeWeights("0.20", "0.40", "0.40"));
        m.put("default", new RegimeWeights("0.20", "0.40", "0.40"));
        return m;
    }

    @Getter
    @Setter
    public static class RegimeWeights {
        private BigDecimal shortPct;
        private BigDecimal midPct;
        private BigDecimal longPct;

        public RegimeWeights() {}

        public RegimeWeights(String shortPct, String midPct, String longPct) {
            this.shortPct = new BigDecimal(shortPct);
            this.midPct = new BigDecimal(midPct);
            this.longPct = new BigDecimal(longPct);
        }
    }
}
