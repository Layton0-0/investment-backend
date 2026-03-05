package com.investment.risk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 매크로 대시보드 응답 DTO.
 */
@Getter
@Builder
public class MacroDashboardResponse {

    /** 현재 시장 상태 (BULL, BEAR, NEUTRAL) */
    private final MarketRegime regime;

    /** 시장 상태 신뢰도 (0~1) */
    private final Double regimeConfidence;

    /** 종합 리스크 점수 (0~100, 낮을수록 안전) */
    private final Integer overallRiskScore;

    /** 시장 지표 */
    private final List<MacroIndicatorDto> marketIndicators;

    /** 금리 지표 */
    private final List<MacroIndicatorDto> interestRateIndicators;

    /** 경제 지표 */
    private final List<MacroIndicatorDto> economyIndicators;

    /** 환율 지표 */
    private final List<MacroIndicatorDto> currencyIndicators;

    /** 모든 지표 (코드 → 지표) */
    private final Map<String, MacroIndicatorDto> allIndicators;

    /** 리스크 게이트 상태 */
    private final RiskGateStatus riskGateStatus;

    /** 조회 시점 (ISO-8601) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant timestamp;

    /** 캐시 여부 */
    private final boolean cached;

    /** 캐시 TTL (초) */
    private final Integer cacheTtlSeconds;

    public enum MarketRegime {
        /** 상승장 - 낮은 변동성, 상승 추세 */
        BULL,
        /** 하락장 - 높은 변동성, 하락 추세 */
        BEAR,
        /** 횡보장 - 중간 변동성, 방향성 없음 */
        NEUTRAL
    }

    @Getter
    @Builder
    public static class RiskGateStatus {
        /** 리스크 게이트 활성화 여부 */
        private final boolean enabled;
        /** 현재 VIX */
        private final Double currentVix;
        /** VIX 임계값 */
        private final Double vixThreshold;
        /** 게이트 트리거 여부 */
        private final boolean triggered;
        /** 비중 축소 비율 (%) */
        private final Integer reduceSizePercent;
    }
}
