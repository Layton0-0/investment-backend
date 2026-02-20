package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 스트레스 테스트 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StressTestResult {

    private String scenarioCode;
    private String scenarioName;
    private String scenarioDescription;

    /**
     * 포트폴리오 전체 예상 손실률 (%).
     */
    private BigDecimal portfolioLossPct;

    /**
     * 포트폴리오 전체 예상 손실액.
     */
    private BigDecimal portfolioLossAmount;

    /**
     * 스트레스 전 포트폴리오 가치.
     */
    private BigDecimal portfolioValueBefore;

    /**
     * 스트레스 후 예상 포트폴리오 가치.
     */
    private BigDecimal portfolioValueAfter;

    /**
     * 개별 종목별 영향.
     */
    private List<SymbolImpact> symbolImpacts;

    /**
     * 자산군별 영향.
     */
    private Map<String, AssetClassImpact> assetClassImpacts;

    /**
     * 스트레스 시나리오 적용 VIX 수준.
     */
    private BigDecimal appliedVixLevel;

    /**
     * 계산 소요 시간 (ms).
     */
    private long elapsedTimeMs;

    /**
     * 계산 시점.
     */
    private Instant calculatedAt;

    /**
     * 유효성 (데이터 충분 여부).
     */
    private boolean valid;

    /**
     * 에러 메시지 (실패 시).
     */
    private String errorMessage;

    /**
     * 리스크 등급 (CRITICAL, HIGH, MEDIUM, LOW).
     */
    private RiskGrade riskGrade;

    public enum RiskGrade {
        CRITICAL,  // 손실 >= 40%
        HIGH,      // 손실 >= 25%
        MEDIUM,    // 손실 >= 10%
        LOW        // 손실 < 10%
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SymbolImpact {
        private String symbol;
        private String market;
        private String assetClass;
        private BigDecimal currentValue;
        private BigDecimal expectedLossPct;
        private BigDecimal expectedLossAmount;
        private BigDecimal valueAfterStress;
        private BigDecimal weight;
        private BigDecimal contributionToPortfolioLoss;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetClassImpact {
        private String assetClass;
        private BigDecimal totalValue;
        private BigDecimal appliedShock;
        private BigDecimal expectedLossAmount;
        private BigDecimal weight;
    }

    public static StressTestResult error(String scenarioCode, String message) {
        return StressTestResult.builder()
                .scenarioCode(scenarioCode)
                .valid(false)
                .errorMessage(message)
                .calculatedAt(Instant.now())
                .build();
    }

    public static StressTestResult insufficientData(String scenarioCode) {
        return StressTestResult.builder()
                .scenarioCode(scenarioCode)
                .valid(false)
                .errorMessage("포트폴리오 데이터 부족")
                .calculatedAt(Instant.now())
                .build();
    }
}
