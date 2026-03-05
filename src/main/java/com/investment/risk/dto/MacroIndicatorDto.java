package com.investment.risk.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 개별 매크로 지표 DTO.
 */
@Getter
@Builder
public class MacroIndicatorDto {

    /** 지표 코드 (예: VIX, MOVE, US10Y) */
    private final String code;

    /** 지표 이름 */
    private final String name;

    /** 지표 분류 (MARKET, INTEREST_RATE, ECONOMY, CURRENCY) */
    private final IndicatorCategory category;

    /** 현재값 */
    private final BigDecimal value;

    /** 전일 대비 변화량 */
    private final BigDecimal change;

    /** 전일 대비 변화율 (%) */
    private final BigDecimal changePercent;

    /** 신호등 (GREEN, YELLOW, RED) */
    private final SignalLevel signal;

    /** 신호 설명 */
    private final String signalDescription;

    /** 데이터 소스 */
    private final String source;

    /** 최종 업데이트 시간 (ISO-8601) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant updatedAt;

    public enum IndicatorCategory {
        /** 시장 지표: VIX, MOVE, Put/Call Ratio */
        MARKET,
        /** 금리 지표: 기준금리, 국채 수익률 */
        INTEREST_RATE,
        /** 경제 지표: PMI, CPI, 실업률 */
        ECONOMY,
        /** 환율 지표: USD/KRW, DXY */
        CURRENCY
    }

    public enum SignalLevel {
        /** 양호 - 리스크 온 */
        GREEN,
        /** 주의 - 모니터링 필요 */
        YELLOW,
        /** 경고 - 리스크 오프 */
        RED
    }
}
