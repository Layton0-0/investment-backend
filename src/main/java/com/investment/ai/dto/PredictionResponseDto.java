package com.investment.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AI ?ˆì¸¡ ?‘ë‹µ DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponseDto {
    
    /**
     * ì¢…ëª© ì½”ë“œ
     */
    private String symbol;
    
    /**
     * ?„ì¬ ê°€ê²?
     */
    private BigDecimal currentPrice;
    
    /**
     * ?ˆì¸¡ ê°€ê²?
     */
    private BigDecimal predictedPrice;
    
    /**
     * ?ˆì¸¡ ê°€ê²?ë²”ìœ„ (?˜í•œ)
     */
    private BigDecimal predictedPriceLower;
    
    /**
     * ?ˆì¸¡ ê°€ê²?ë²”ìœ„ (?í•œ)
     */
    private BigDecimal predictedPriceUpper;
    
    /**
     * ?ˆìƒ ?˜ìµë¥?(%)
     */
    private BigDecimal expectedReturn;
    
    /**
     * ?ˆì¸¡ ? ë¢°??(0.0 ~ 1.0)
     */
    private BigDecimal confidence;
    
    /**
     * ?ˆìƒ ë³€?™ì„± (Volatility)
     */
    private BigDecimal volatility;
    
    /**
     * ?ˆì¸¡ ë°©í–¥ (UP, DOWN, NEUTRAL)
     */
    private String direction;
    
    /**
     * ?¬ìš©??ëª¨ë¸ ?€??
     */
    private String modelType;
    
    /**
     * ?ˆì¸¡ ?œê°
     */
    private LocalDateTime predictedAt;
    
    /**
     * ?ˆì¸¡ ê¸°ê°„ (ë¶?
     */
    private Integer predictionMinutes;
    
    /**
     * ì¶”ê? ë©”í??°ì´??
     */
    private PredictionMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PredictionMetadata {
        /**
         * ëª¨ë¸ ë²„ì „
         */
        private String modelVersion;
        
        /**
         * ?ˆì¸¡???¬ìš©???¼ì²˜ ëª©ë¡
         */
        private List<String> features;
        
        /**
         * ëª¨ë¸ë³??ˆì¸¡ ê²°ê³¼ (?™ìƒë¸”ì¸ ê²½ìš°)
         */
        private List<ModelPrediction> modelPredictions;
        
        /**
         * ?ˆì¸¡ ê·¼ê±° ?¤ëª…
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
