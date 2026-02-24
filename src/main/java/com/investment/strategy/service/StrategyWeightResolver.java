package com.investment.strategy.service;

import com.investment.config.StrategyWeightProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.strategy.dto.StrategyWeights;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 시장 레짐에 따른 전략 비중(단기/중기/장기) 동적 결정.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyWeightResolver {

    private static final BigDecimal DEFAULT_SHORT = new BigDecimal("0.2");
    private static final BigDecimal DEFAULT_MID = new BigDecimal("0.4");
    private static final BigDecimal DEFAULT_LONG = new BigDecimal("0.4");

    private final StrategyWeightProperties properties;
    private final MacroEconomicStrategyEngine macroEngine;

    /**
     * 설정과 지표를 반영해 단기/중기/장기 비중 결정. 합=1.
     *
     * @param setting   계좌 거래 설정 (비활성 시 또는 fallback 시 사용)
     * @param indicators 거시 지표 (없으면 기본 비중)
     * @return shortPct, midPct, longPct (합=1), regime(감사용)
     */
    public StrategyWeights resolve(TradingSetting setting,
                                    Optional<MacroEconomicStrategyEngine.MacroEconomicIndicators> indicators) {
        if (!properties.isEnabled()) {
            return fromSettingOrDefault(setting, "DISABLED");
        }
        if (indicators == null || indicators.isEmpty()) {
            return fromSettingOrDefault(setting, "NO_INDICATORS");
        }
        try {
            MacroEconomicStrategyEngine.InvestmentStrategy strategy = macroEngine.decideStrategy(indicators.get());
            String regimeName = strategy.getRegime() != null ? strategy.getRegime().name() : "NORMAL";
            StrategyWeightProperties.RegimeWeights target = properties.getRegimeWeights().get(regimeName);
            if (target == null) {
                target = properties.getRegimeWeights().get("default");
            }
            if (target == null) {
                return fromSettingOrDefault(setting, regimeName);
            }
            BigDecimal s = clip(target.getShortPct());
            BigDecimal m = clip(target.getMidPct());
            BigDecimal l = clip(target.getLongPct());
            BigDecimal[] normalized = normalizeToOne(s, m, l);
            StrategyWeights result = StrategyWeights.builder()
                    .shortPct(normalized[0])
                    .midPct(normalized[1])
                    .longPct(normalized[2])
                    .regime(regimeName)
                    .build();
            log.debug("전략 비중 결정: regime={}, short={}, mid={}, long={}",
                    regimeName, normalized[0], normalized[1], normalized[2]);
            return result;
        } catch (Exception e) {
            log.warn("전략 비중 레짐 결정 실패, 설정/기본값 사용: {}", e.getMessage());
            return fromSettingOrDefault(setting, "ERROR");
        }
    }

    private StrategyWeights fromSettingOrDefault(TradingSetting setting, String regime) {
        BigDecimal s = setting != null && setting.getShortTermRatio() != null ? setting.getShortTermRatio() : DEFAULT_SHORT;
        BigDecimal m = setting != null && setting.getMediumTermRatio() != null ? setting.getMediumTermRatio() : DEFAULT_MID;
        BigDecimal l = setting != null && setting.getLongTermRatio() != null ? setting.getLongTermRatio() : DEFAULT_LONG;
        BigDecimal sum = s.add(m).add(l);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            s = DEFAULT_SHORT;
            m = DEFAULT_MID;
            l = DEFAULT_LONG;
            sum = BigDecimal.ONE;
        }
        BigDecimal scale = BigDecimal.ONE.divide(sum, 6, RoundingMode.HALF_UP);
        return StrategyWeights.builder()
                .shortPct(s.multiply(scale).setScale(4, RoundingMode.HALF_UP))
                .midPct(m.multiply(scale).setScale(4, RoundingMode.HALF_UP))
                .longPct(l.multiply(scale).setScale(4, RoundingMode.HALF_UP))
                .regime(regime)
                .build();
    }

    private BigDecimal clip(BigDecimal value) {
        if (value == null) return DEFAULT_SHORT;
        BigDecimal min = properties.getMinWeight() != null ? properties.getMinWeight() : new BigDecimal("0.05");
        BigDecimal max = properties.getMaxWeight() != null ? properties.getMaxWeight() : new BigDecimal("0.55");
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }

    private BigDecimal[] normalizeToOne(BigDecimal s, BigDecimal m, BigDecimal l) {
        BigDecimal sum = s.add(m).add(l);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal[]{DEFAULT_SHORT, DEFAULT_MID, DEFAULT_LONG};
        }
        int scale = 4;
        RoundingMode mode = RoundingMode.HALF_UP;
        BigDecimal ss = s.divide(sum, scale, mode);
        BigDecimal mm = m.divide(sum, scale, mode);
        BigDecimal ll = l.divide(sum, scale, mode);
        BigDecimal total = ss.add(mm).add(ll);
        if (total.compareTo(BigDecimal.ONE) != 0) {
            ll = BigDecimal.ONE.subtract(ss).subtract(mm);
        }
        return new BigDecimal[]{ss, mm, ll};
    }
}
