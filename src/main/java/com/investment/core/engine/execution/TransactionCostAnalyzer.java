package com.investment.core.engine.execution;

import com.investment.core.engine.execution.dto.TcaEstimateRequest;
import com.investment.core.engine.execution.dto.TcaReport;

import java.math.BigDecimal;
import java.util.List;

/**
 * Transaction Cost Analysis (TCA) 서비스 인터페이스.
 * 거래 비용을 사전 예측하고 사후 분석합니다.
 */
public interface TransactionCostAnalyzer {

    /**
     * 사전 비용 예측 (Pre-Trade Analysis).
     * 주문 실행 전 예상되는 총 거래 비용을 계산합니다.
     *
     * @param request 비용 예측 요청
     * @return TCA 리포트 (예상 비용 breakdown)
     */
    TcaReport estimateCost(TcaEstimateRequest request);

    /**
     * 사후 비용 분석 (Post-Trade Analysis).
     * 실제 체결 결과와 도착가를 비교하여 Implementation Shortfall을 계산합니다.
     *
     * @param symbol         종목 코드
     * @param market         시장 (KR, US)
     * @param side           매수/매도
     * @param quantity       주문 수량
     * @param arrivalPrice   도착가 (주문 결정 시점 가격)
     * @param executionPrice 체결 평균가
     * @param executionCost  실제 발생한 거래 비용
     * @return TCA 리포트 (실제 비용 분석)
     */
    TcaReport analyzeCost(String symbol, String market, String side,
                          int quantity, BigDecimal arrivalPrice,
                          BigDecimal executionPrice, BigDecimal executionCost);

    /**
     * Implementation Shortfall 계산.
     * IS = (실행가 - 도착가) × 수량 + 거래비용
     *
     * @param arrivalPrice   도착가
     * @param executionPrice 체결가
     * @param quantity       수량
     * @param side           매수/매도
     * @param transactionCost 거래 비용
     * @return Implementation Shortfall (금액)
     */
    BigDecimal calculateImplementationShortfall(BigDecimal arrivalPrice,
                                                 BigDecimal executionPrice,
                                                 int quantity,
                                                 String side,
                                                 BigDecimal transactionCost);

    /**
     * 시장 충격 비용 예측 (Market Impact Model).
     * sqrt(Volume/ADV) 기반 근사 모델.
     *
     * @param orderQuantity   주문 수량
     * @param avgDailyVolume  일평균 거래량
     * @param volatility      변동성 (선택, 기본 0.02)
     * @return 시장 충격 비용 (%)
     */
    BigDecimal estimateMarketImpact(int orderQuantity, long avgDailyVolume, BigDecimal volatility);

    /**
     * 왕복 거래 비용 계산 (Round-Trip Cost).
     * 매수 + 매도 시 발생하는 총 비용.
     *
     * @param market    시장 (KR, US)
     * @param assetType 자산 유형 (STOCK, ETF)
     * @param notional  거래 금액
     * @param quantity  수량 (TAF 계산용)
     * @return 왕복 비용 (금액)
     */
    BigDecimal calculateRoundTripCost(String market, String assetType,
                                       BigDecimal notional, int quantity);
}
