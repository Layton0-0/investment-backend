package com.investment.core.engine.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 세금·비용 감안 포트폴리오 최적화.
 * 국내: 거래세·수수료 반영; 해외: 양도소득세·Tax Harvesting 고려.
 * Phase 2에서 구현.
 */
public interface TaxAwareOptimizer {

    /**
     * 비용·세금 반영 후 목표 비중 조정.
     *
     * @param asOfDate     기준일
     * @param market       시장 (KR, US)
     * @param rawWeights   전략에서 나온 원시 비중 (symbol -> weight 0~1)
     * @return 조정된 목표 비중 (기대 수익이 비용을 상쇄하지 않으면 해당 종목 0)
     */
    Map<String, BigDecimal> optimizeWeights(LocalDate asOfDate, String market, Map<String, BigDecimal> rawWeights);
}
