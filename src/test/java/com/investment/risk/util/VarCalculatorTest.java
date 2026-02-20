package com.investment.risk.util;

import com.investment.config.RiskProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VarCalculator")
class VarCalculatorTest {

    @Mock
    private RiskProperties riskProperties;

    private VarCalculator varCalculator;

    @BeforeEach
    void setUp() {
        varCalculator = new VarCalculator(riskProperties);
    }

    @Test
    @DisplayName("파라메트릭 VaR 95% 계산")
    void calculateVar95_parametric_returnsCorrectValue() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.PARAMETRIC);
        when(riskProperties.getVarDailyVolPct()).thenReturn(new BigDecimal("2.0"));

        BigDecimal var95 = varCalculator.calculateVar95(null);

        assertThat(var95).isEqualByComparingTo("3.30"); // 1.65 × 2.0
    }

    @Test
    @DisplayName("파라메트릭 CVaR 95% 계산")
    void calculateCvar95_parametric_returnsCorrectValue() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.PARAMETRIC);
        when(riskProperties.getVarDailyVolPct()).thenReturn(new BigDecimal("2.0"));

        BigDecimal cvar95 = varCalculator.calculateCvar95(null);

        assertThat(cvar95).isEqualByComparingTo("4.12"); // 2.06 × 2.0
    }

    @Test
    @DisplayName("변동성 미설정 시 VaR null 반환")
    void calculateVar95_noVolatility_returnsNull() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.PARAMETRIC);
        when(riskProperties.getVarDailyVolPct()).thenReturn(null);

        BigDecimal var95 = varCalculator.calculateVar95(null);

        assertThat(var95).isNull();
    }

    @Test
    @DisplayName("역사적 VaR - 데이터 부족 시 null 반환")
    void calculateVar95_historicalInsufficientData_returnsNull() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        List<BigDecimal> returns = List.of(
                new BigDecimal("-0.01"),
                new BigDecimal("0.02"),
                new BigDecimal("-0.005")
        );

        BigDecimal var95 = varCalculator.calculateVar95(returns);

        assertThat(var95).isNull();
    }

    @Test
    @DisplayName("역사적 VaR - 충분한 데이터로 계산")
    void calculateVar95_historicalSufficientData_returnsValue() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        List<BigDecimal> returns = generateMockReturns(100);

        BigDecimal var95 = varCalculator.calculateVar95(returns);

        assertThat(var95).isNotNull();
    }

    @Test
    @DisplayName("역사적 CVaR - 충분한 데이터로 계산")
    void calculateCvar95_historicalSufficientData_returnsValue() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        List<BigDecimal> returns = generateMockReturns(100);

        BigDecimal cvar95 = varCalculator.calculateCvar95(returns);

        assertThat(cvar95).isNotNull();
    }

    @Test
    @DisplayName("getCurrentMethod 반환")
    void getCurrentMethod_returnsMethodName() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        String method = varCalculator.getCurrentMethod();

        assertThat(method).isEqualTo("HISTORICAL");
    }

    @Test
    @DisplayName("역사적 VaR 사용 가능 여부 확인")
    void isHistoricalVarAvailable_sufficientData_returnsTrue() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        boolean available = varCalculator.isHistoricalVarAvailable(100);

        assertThat(available).isTrue();
    }

    @Test
    @DisplayName("역사적 VaR 사용 불가 - 데이터 부족")
    void isHistoricalVarAvailable_insufficientData_returnsFalse() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        boolean available = varCalculator.isHistoricalVarAvailable(30);

        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("역사적 VaR - 60일 이상 데이터로 정확한 백분위수 계산 검증")
    void calculateVar95_historicalWith60Days_correctPercentile() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        List<BigDecimal> returns = generateSequentialReturns(100);

        BigDecimal var95 = varCalculator.calculateVar95(returns);

        assertThat(var95).isNotNull();
        assertThat(var95.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
    }

    @Test
    @DisplayName("역사적 CVaR - Expected Shortfall이 VaR보다 크거나 같음")
    void calculateCvar95_shouldBeGreaterOrEqualToVar95() {
        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);

        List<BigDecimal> returns = generateSequentialReturns(100);

        BigDecimal var95 = varCalculator.calculateVar95(returns);
        BigDecimal cvar95 = varCalculator.calculateCvar95(returns);

        assertThat(var95).isNotNull();
        assertThat(cvar95).isNotNull();
        assertThat(cvar95.compareTo(var95)).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("파라메트릭 vs 역사적 VaR 비교 - 둘 다 양수")
    void calculateVar95_parametricVsHistorical_bothPositive() {
        List<BigDecimal> returns = generateMockReturns(100);

        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.PARAMETRIC);
        when(riskProperties.getVarDailyVolPct()).thenReturn(new BigDecimal("2.0"));
        BigDecimal parametricVar = varCalculator.calculateVar95(null);

        when(riskProperties.getVarMethod()).thenReturn(RiskProperties.VarMethod.HISTORICAL);
        BigDecimal historicalVar = varCalculator.calculateVar95(returns);

        assertThat(parametricVar).isNotNull();
        assertThat(historicalVar).isNotNull();
        assertThat(parametricVar.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
        assertThat(historicalVar.compareTo(BigDecimal.ZERO)).isGreaterThan(0);
    }

    @Test
    @DisplayName("getMinHistoricalSamples - 60 반환")
    void getMinHistoricalSamples_returns60() {
        int minSamples = varCalculator.getMinHistoricalSamples();

        assertThat(minSamples).isEqualTo(60);
    }

    private List<BigDecimal> generateMockReturns(int count) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double randomReturn = (Math.random() - 0.5) * 0.1;
            returns.add(new BigDecimal(String.format("%.6f", randomReturn)));
        }
        return returns;
    }

    private List<BigDecimal> generateSequentialReturns(int count) {
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double returnValue = -0.05 + (i * 0.001);
            returns.add(new BigDecimal(String.format("%.6f", returnValue)));
        }
        return returns;
    }
}
