package com.investment.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 예측 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponseDto {

    /**
     * 종목 심볼
     */
    private String symbol;

    /**
     * 현재 가격
     */
    private BigDecimal currentPrice;

    /**
     * 예측 가격
     */
    private BigDecimal predictedPrice;

    /**
     * 예측 가격 범위 (하한)
     */
    private BigDecimal predictedPriceLower;

    /**
     * 예측 가격 범위 (상한)
     */
    private BigDecimal predictedPriceUpper;

    /**
     * 예상 수익률 (%)
     */
    private BigDecimal expectedReturn;

    /**
     * 예측 신뢰도 (0.0 ~ 1.0)
     */
    private BigDecimal confidence;

    /**
     * 예상 변동성 (Volatility)
     */
    private BigDecimal volatility;

    /**
     * 예측 방향 (UP, DOWN, NEUTRAL)
     */
    private String direction;

    /**
     * 사용된 모델 타입
     */
    private String modelType;

    /**
     * 예측 시점
     */
    private LocalDateTime predictedAt;

    /**
     * 예측 기간 (분)
     */
    private Integer predictionMinutes;

    /**
     * 추가 메타데이터
     */
    private PredictionMetadata metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionMetadata {
        /**
         * 모델 버전
         */
        private String modelVersion;

        /**
         * 예측에 사용된 특성 목록
         */
        private List<String> features;

        /**
         * 모델별 예측 결과 (앙상블 가중치)
         */
        private List<ModelPrediction> modelPredictions;

        /**
         * 예측 근거 설명
         */
        private String reasoning;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelPrediction {
        private String modelName;
        private BigDecimal predictedPrice;
        private BigDecimal weight;
    }
}