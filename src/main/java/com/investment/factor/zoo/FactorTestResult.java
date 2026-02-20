package com.investment.factor.zoo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Factor Zoo 팩터 테스트 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorTestResult {

    private String factorCode;
    private String factorName;
    private FactorDefinition.FactorCategory category;
    private FactorDefinition.FactorDirection direction;

    private LocalDate testStartDate;
    private LocalDate testEndDate;
    private String market;
    private int universeSize;

    /**
     * Information Coefficient (IC): 팩터 값과 미래 수익률 간 상관계수.
     * 양수: 팩터 높은 종목이 수익률 높음 (HIGHER_BETTER 팩터에 유효).
     * 일반적으로 |IC| > 0.02~0.03이면 유의미.
     */
    private BigDecimal informationCoefficient;

    /**
     * IC의 t-statistic. |t| > 2이면 통계적으로 유의.
     */
    private BigDecimal icTStat;

    /**
     * IC의 표준편차.
     */
    private BigDecimal icStdDev;

    /**
     * Information Ratio (IR): IC 평균 / IC 표준편차.
     * |IR| > 0.5이면 우수.
     */
    private BigDecimal informationRatio;

    /**
     * 팩터 Turnover: 리밸런싱 시 종목 교체 비율 (%).
     * 높을수록 거래비용 증가.
     */
    private BigDecimal turnoverPct;

    /**
     * IC Decay: 기간 경과에 따른 IC 감소율.
     * 0에 가까울수록 안정적.
     */
    private BigDecimal icDecay;

    /**
     * 분위별 수익률 (예: Q1~Q5 또는 D1~D10).
     */
    private List<QuantileReturn> quantileReturns;

    /**
     * 롱숏 스프레드: Top 분위 - Bottom 분위 수익률 (%).
     */
    private BigDecimal longShortSpreadPct;

    /**
     * 기간별 IC 시계열.
     */
    private Map<LocalDate, BigDecimal> icTimeSeries;

    /**
     * 테스트 유효성.
     */
    private boolean valid;

    /**
     * 에러 메시지.
     */
    private String errorMessage;

    /**
     * 계산 시점.
     */
    private Instant calculatedAt;

    /**
     * 팩터 등급 (A/B/C/D/F).
     */
    private FactorGrade grade;

    public enum FactorGrade {
        A,  // IC >= 0.05, IR >= 0.5
        B,  // IC >= 0.03, IR >= 0.3
        C,  // IC >= 0.02, IR >= 0.2
        D,  // IC >= 0.01
        F   // IC < 0.01 or invalid
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuantileReturn {
        private int quantile;
        private String label;
        private int stockCount;
        private BigDecimal avgReturnPct;
        private BigDecimal cumulativeReturnPct;
    }

    public static FactorTestResult error(String factorCode, String message) {
        return FactorTestResult.builder()
                .factorCode(factorCode)
                .valid(false)
                .errorMessage(message)
                .grade(FactorGrade.F)
                .calculatedAt(Instant.now())
                .build();
    }

    public static FactorTestResult insufficientData(String factorCode) {
        return FactorTestResult.builder()
                .factorCode(factorCode)
                .valid(false)
                .errorMessage("데이터 부족")
                .grade(FactorGrade.F)
                .calculatedAt(Instant.now())
                .build();
    }
}
