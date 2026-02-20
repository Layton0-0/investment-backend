package com.investment.core.engine.execution.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * TCA (Transaction Cost Analysis) 분석 결과 DTO.
 * 명시적 비용, 암묵적 비용, Implementation Shortfall을 포함합니다.
 */
@Getter
@Builder
public class TcaReport {

    /** 종목 코드 */
    private final String symbol;

    /** 시장 */
    private final String market;

    /** 매수/매도 */
    private final String side;

    /** 주문 수량 */
    private final int quantity;

    /** 도착가 (Arrival Price) - 주문 결정 시점 가격 */
    private final BigDecimal arrivalPrice;

    /** 실행가 (Execution Price) - 실제 체결 평균가 (사후 분석 시) */
    private final BigDecimal executionPrice;

    // === 명시적 비용 (Explicit Costs) ===

    /** 위탁수수료 (금액) */
    private final BigDecimal commissionCost;

    /** 위탁수수료 (%) */
    private final BigDecimal commissionPct;

    /** 세금 (매도 시 거래세/SEC Fee) (금액) */
    private final BigDecimal taxCost;

    /** 세금 (%) */
    private final BigDecimal taxPct;

    /** TAF (미국 매도 시, 금액) */
    private final BigDecimal tafCost;

    /** 명시적 비용 합계 (금액) */
    private final BigDecimal totalExplicitCost;

    /** 명시적 비용 합계 (%) */
    private final BigDecimal totalExplicitPct;

    // === 암묵적 비용 (Implicit Costs) ===

    /** 스프레드 비용 (금액) */
    private final BigDecimal spreadCost;

    /** 스프레드 비용 (%) */
    private final BigDecimal spreadPct;

    /** 슬리피지 비용 (금액) */
    private final BigDecimal slippageCost;

    /** 슬리피지 비용 (%) */
    private final BigDecimal slippagePct;

    /** 시장 충격 비용 (Market Impact) (금액) */
    private final BigDecimal marketImpactCost;

    /** 시장 충격 비용 (%) */
    private final BigDecimal marketImpactPct;

    /** 암묵적 비용 합계 (금액) */
    private final BigDecimal totalImplicitCost;

    /** 암묵적 비용 합계 (%) */
    private final BigDecimal totalImplicitPct;

    // === 총 비용 ===

    /** 총 예상 비용 (금액) */
    private final BigDecimal totalEstimatedCost;

    /** 총 예상 비용 (%) */
    private final BigDecimal totalEstimatedPct;

    // === Implementation Shortfall (사후 분석 시) ===

    /** Implementation Shortfall (금액) - 도착가 대비 실제 체결 비용 */
    private final BigDecimal implementationShortfall;

    /** Implementation Shortfall (%) */
    private final BigDecimal implementationShortfallPct;

    /** 분석 유형 (PRE_TRADE, POST_TRADE) */
    private final AnalysisType analysisType;

    /** 분석 시점 */
    private final Instant analyzedAt;

    /** 결과 유효 여부 */
    private final boolean valid;

    /** 에러 메시지 */
    private final String errorMessage;

    public enum AnalysisType {
        /** 사전 비용 예측 */
        PRE_TRADE,
        /** 사후 비용 분석 */
        POST_TRADE
    }

    public static TcaReport error(String message) {
        return TcaReport.builder()
                .valid(false)
                .errorMessage(message)
                .analyzedAt(Instant.now())
                .build();
    }
}
