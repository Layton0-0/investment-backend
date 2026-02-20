package com.investment.core.engine.execution.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * TCA 사전 비용 예측 요청 DTO.
 */
@Getter
@Builder
public class TcaEstimateRequest {

    /** 종목 코드 */
    private final String symbol;

    /** 시장 (KR, US) */
    private final String market;

    /** 자산 유형 (STOCK, ETF) */
    private final String assetType;

    /** 매수/매도 (BUY, SELL) */
    private final String side;

    /** 주문 수량 */
    private final int quantity;

    /** 주문 예상 가격 (진입가/도착가) */
    private final BigDecimal arrivalPrice;

    /** 일평균 거래량 (ADV) - Market Impact 계산용 */
    private final Long avgDailyVolume;

    /** 평균 스프레드 (%) - 선택적, 미제공 시 기본값 사용 */
    private final BigDecimal avgSpreadPct;
}
