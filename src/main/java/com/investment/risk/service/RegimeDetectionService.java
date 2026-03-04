package com.investment.risk.service;

import com.investment.risk.dto.MacroDashboardResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 시장 레짐(Bull/Bear/Sideways) 탐지 — VIX + SPY 이평선 기반 규칙 엔진.
 * BULL = SPY 50일선 &gt; 200일선 AND VIX &lt; 20.
 * BEAR = SPY 50일선 &lt; 200일선 AND VIX &gt; 30.
 * 그 외 NEUTRAL.
 * 결과는 Redis 캐시(1시간 TTL)하며 RiskGateService에서 활용.
 */
public interface RegimeDetectionService {

    /**
     * 현재 시장 레짐 반환. 데이터 부족 시 NEUTRAL.
     *
     * @param asOfDate 기준일 (null이면 오늘)
     * @return 레짐 및 신뢰도
     */
    RegimeResult getCurrentRegime(LocalDate asOfDate);

    /**
     * 레짐 + 신뢰도
     */
    record RegimeResult(
            MacroDashboardResponse.MarketRegime regime,
            double confidence,
            BigDecimal spyMa50,
            BigDecimal spyMa200,
            BigDecimal vix
    ) {}
}
