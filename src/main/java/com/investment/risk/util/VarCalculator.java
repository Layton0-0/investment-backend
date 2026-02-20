package com.investment.risk.util;

import com.investment.config.RiskProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * VaR/CVaR 계산 유틸리티.
 * 파라메트릭 및 역사적 방법론 지원.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VarCalculator {

    private static final BigDecimal PARAMETRIC_VAR_95_MULTIPLIER = new BigDecimal("1.65");
    private static final BigDecimal PARAMETRIC_CVAR_95_MULTIPLIER = new BigDecimal("2.06");
    private static final int MIN_HISTORICAL_SAMPLES = 60;

    private final RiskProperties riskProperties;

    /**
     * 95% VaR 계산.
     * @param dailyReturns 일별 수익률 목록 (역사적 VaR용). null이면 파라메트릭만 사용.
     * @return VaR 95% (%)
     */
    public BigDecimal calculateVar95(List<BigDecimal> dailyReturns) {
        RiskProperties.VarMethod method = riskProperties.getVarMethod();
        
        if (method == RiskProperties.VarMethod.HISTORICAL && dailyReturns != null && dailyReturns.size() >= MIN_HISTORICAL_SAMPLES) {
            return calculateHistoricalVar95(dailyReturns);
        }
        
        return calculateParametricVar95();
    }

    /**
     * 95% CVaR (Expected Shortfall) 계산.
     * @param dailyReturns 일별 수익률 목록 (역사적 VaR용). null이면 파라메트릭만 사용.
     * @return CVaR 95% (%)
     */
    public BigDecimal calculateCvar95(List<BigDecimal> dailyReturns) {
        RiskProperties.VarMethod method = riskProperties.getVarMethod();
        
        if (method == RiskProperties.VarMethod.HISTORICAL && dailyReturns != null && dailyReturns.size() >= MIN_HISTORICAL_SAMPLES) {
            return calculateHistoricalCvar95(dailyReturns);
        }
        
        return calculateParametricCvar95();
    }

    /**
     * 파라메트릭 VaR 95% (정규분포 가정).
     * VaR = 1.65 × σ
     */
    private BigDecimal calculateParametricVar95() {
        BigDecimal vol = riskProperties.getVarDailyVolPct();
        if (vol == null || vol.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return PARAMETRIC_VAR_95_MULTIPLIER.multiply(vol).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 파라메트릭 CVaR 95% (정규분포 가정).
     * CVaR = 2.06 × σ
     */
    private BigDecimal calculateParametricCvar95() {
        BigDecimal vol = riskProperties.getVarDailyVolPct();
        if (vol == null || vol.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return PARAMETRIC_CVAR_95_MULTIPLIER.multiply(vol).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 역사적 VaR 95% (5번째 백분위수).
     * @param dailyReturns 일별 수익률 목록 (음수 = 손실)
     */
    private BigDecimal calculateHistoricalVar95(List<BigDecimal> dailyReturns) {
        if (dailyReturns == null || dailyReturns.size() < MIN_HISTORICAL_SAMPLES) {
            log.debug("역사적 VaR 계산 불가: 데이터 부족 (최소 {}일 필요, 현재 {}일)", 
                    MIN_HISTORICAL_SAMPLES, dailyReturns != null ? dailyReturns.size() : 0);
            return null;
        }

        List<BigDecimal> sorted = dailyReturns.stream()
                .sorted()
                .toList();
        
        int index = (int) Math.ceil(sorted.size() * 0.05) - 1;
        index = Math.max(0, index);
        
        BigDecimal var95 = sorted.get(index).negate();
        return var95.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 역사적 CVaR 95% (5번째 백분위수 이하의 평균 손실).
     * @param dailyReturns 일별 수익률 목록 (음수 = 손실)
     */
    private BigDecimal calculateHistoricalCvar95(List<BigDecimal> dailyReturns) {
        if (dailyReturns == null || dailyReturns.size() < MIN_HISTORICAL_SAMPLES) {
            return null;
        }

        List<BigDecimal> sorted = dailyReturns.stream()
                .sorted()
                .toList();
        
        int cutoffIndex = (int) Math.ceil(sorted.size() * 0.05);
        cutoffIndex = Math.max(1, cutoffIndex);
        
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < cutoffIndex; i++) {
            sum = sum.add(sorted.get(i).negate());
        }
        
        BigDecimal cvar95 = sum.divide(new BigDecimal(cutoffIndex), 4, RoundingMode.HALF_UP);
        return cvar95.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 현재 설정된 VaR 방법론 반환.
     */
    public String getCurrentMethod() {
        RiskProperties.VarMethod method = riskProperties.getVarMethod();
        return method != null ? method.name() : "PARAMETRIC";
    }

    /**
     * 역사적 VaR 사용 가능 여부.
     * @param dataCount 사용 가능한 데이터 개수
     */
    public boolean isHistoricalVarAvailable(int dataCount) {
        return riskProperties.getVarMethod() == RiskProperties.VarMethod.HISTORICAL 
                && dataCount >= MIN_HISTORICAL_SAMPLES;
    }

    /**
     * 역사적 VaR에 필요한 최소 샘플 수 반환.
     */
    public int getMinHistoricalSamples() {
        return MIN_HISTORICAL_SAMPLES;
    }
}
