package com.investment.core.engine.portfolio;

import com.investment.config.FrictionCostProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 2: 세금·비용 감안 포트폴리오 최적화.
 * KR: 거래세·수수료·슬리피지; US: 수수료·SEC·TAF·슬리피지 반영.
 * 왕복 비용을 상회하지 않는 비중만 유지하고 재정규화.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(TaxAwareOptimizer.class)
@RequiredArgsConstructor
public class TaxAwareOptimizerImpl implements TaxAwareOptimizer {

    private final FrictionCostProperties frictionCostProperties;

    @Override
    public Map<String, BigDecimal> optimizeWeights(LocalDate asOfDate, String market,
            Map<String, BigDecimal> rawWeights) {
        if (rawWeights == null || rawWeights.isEmpty()) {
            return Map.of();
        }

        BigDecimal roundTripCost = roundTripCostRate(market);
        Map<String, BigDecimal> effective = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> e : rawWeights.entrySet()) {
            BigDecimal w = e.getValue() != null ? e.getValue() : BigDecimal.ZERO;
            BigDecimal net = w.multiply(BigDecimal.ONE.subtract(roundTripCost)).setScale(6, RoundingMode.HALF_UP);
            if (net.compareTo(BigDecimal.ZERO) > 0) {
                effective.put(e.getKey(), net);
            }
        }
        if (effective.isEmpty()) {
            return Map.of();
        }
        BigDecimal sum = effective.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of();
        }
        return effective.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().divide(sum, 6, RoundingMode.HALF_UP),
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    private BigDecimal roundTripCostRate(String market) {
        if ("US".equalsIgnoreCase(market)) {
            FrictionCostProperties.UsaStockFee usa = frictionCostProperties.getUsa().getStock();
            BigDecimal c = usa.getCommission() != null ? usa.getCommission() : BigDecimal.ZERO;
            BigDecimal s = usa.getSlippage() != null ? usa.getSlippage() : BigDecimal.ZERO;
            BigDecimal sec = usa.getSecFee() != null ? usa.getSecFee() : BigDecimal.ZERO;
            return c.multiply(BigDecimal.valueOf(2)).add(s.multiply(BigDecimal.valueOf(2))).add(sec);
        }
        FrictionCostProperties.StockFee kr = frictionCostProperties.getKorea().getStock();
        BigDecimal c = kr.getCommission() != null ? kr.getCommission() : BigDecimal.ZERO;
        BigDecimal t = kr.getTax() != null ? kr.getTax() : BigDecimal.ZERO;
        BigDecimal s = kr.getSlippage() != null ? kr.getSlippage() : BigDecimal.ZERO;
        return c.multiply(BigDecimal.valueOf(2)).add(t).add(s.multiply(BigDecimal.valueOf(2)));
    }
}
