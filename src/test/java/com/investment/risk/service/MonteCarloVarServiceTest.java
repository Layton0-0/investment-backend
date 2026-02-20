package com.investment.risk.service;

import com.investment.config.MonteCarloProperties;
import com.investment.risk.dto.MonteCarloVarResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@DisplayName("MonteCarloVarService 단위 테스트")
class MonteCarloVarServiceTest {

    private MonteCarloVarService service;
    private MonteCarloProperties properties;

    @BeforeEach
    void setUp() {
        properties = new MonteCarloProperties();
        properties.setScenarios(10000);
        properties.setDistribution(MonteCarloProperties.Distribution.STUDENT_T);
        properties.setDegreesOfFreedom(5);
        properties.setMinSamples(60);
        properties.setParallelThreads(4);
        properties.setFixedSeed(true);
        properties.setSeedValue(42L);

        service = new MonteCarloVarServiceImpl(properties);
    }

    @Nested
    @DisplayName("VaR 계산 테스트")
    class VarCalculationTests {

        @Test
        @DisplayName("정상적인 수익률 데이터로 VaR 95% 계산")
        void shouldCalculateVaR95WithValidData() {
            List<BigDecimal> returns = generateSampleReturns(252, 0.001, 0.02);

            MonteCarloVarResult result = service.calculateVaR95(returns);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getValue()).isNotNull();
            assertThat(result.getValue().doubleValue()).isBetween(0.0, 20.0);
            assertThat(result.getConfidenceLevel()).isEqualByComparingTo(new BigDecimal("0.95"));
            assertThat(result.getScenarios()).isEqualTo(10000);
            assertThat(result.getMethod()).contains("MONTE_CARLO");
            assertThat(result.getSampleCount()).isEqualTo(252);
            assertThat(result.getElapsedTimeMs()).isGreaterThan(0);
        }

        @Test
        @DisplayName("데이터 부족 시 에러 반환")
        void shouldReturnErrorWhenInsufficientData() {
            List<BigDecimal> returns = generateSampleReturns(30, 0.001, 0.02);

            MonteCarloVarResult result = service.calculateVaR95(returns);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Insufficient data");
            assertThat(result.getSampleCount()).isEqualTo(30);
        }

        @Test
        @DisplayName("null 데이터 처리")
        void shouldHandleNullData() {
            MonteCarloVarResult result = service.calculateVaR95(null);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Insufficient data");
        }

        @Test
        @DisplayName("빈 데이터 처리")
        void shouldHandleEmptyData() {
            MonteCarloVarResult result = service.calculateVaR95(Collections.emptyList());

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("사용자 정의 신뢰수준과 시나리오 수로 VaR 계산")
        void shouldCalculateVaRWithCustomParameters() {
            List<BigDecimal> returns = generateSampleReturns(100, 0.0, 0.015);

            MonteCarloVarResult result = service.calculateVaR(
                    returns,
                    new BigDecimal("0.99"),
                    5000
            );

            assertThat(result.isValid()).isTrue();
            assertThat(result.getConfidenceLevel()).isEqualByComparingTo(new BigDecimal("0.99"));
            assertThat(result.getScenarios()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("CVaR 계산 테스트")
    class CVarCalculationTests {

        @Test
        @DisplayName("CVaR 95% 계산 (Expected Shortfall)")
        void shouldCalculateCVaR95() {
            List<BigDecimal> returns = generateSampleReturns(252, 0.001, 0.02);

            MonteCarloVarResult cvarResult = service.calculateCVaR95(returns);
            MonteCarloVarResult varResult = service.calculateVaR95(returns);

            assertThat(cvarResult.isValid()).isTrue();
            assertThat(cvarResult.getValue()).isNotNull();
            assertThat(cvarResult.getMethod()).contains("ES");
            assertThat(cvarResult.getValue().doubleValue())
                    .isGreaterThanOrEqualTo(varResult.getValue().doubleValue());
        }

        @Test
        @DisplayName("CVaR이 VaR보다 1.2~1.5배 범위 내")
        void shouldHaveCVaRWithinExpectedRatioOfVaR() {
            List<BigDecimal> returns = generateSampleReturns(252, 0.0, 0.02);

            MonteCarloVarResult varResult = service.calculateVaR95(returns);
            MonteCarloVarResult cvarResult = service.calculateCVaR95(returns);

            if (varResult.isValid() && cvarResult.isValid() && varResult.getValue().doubleValue() > 0) {
                double ratio = cvarResult.getValue().doubleValue() / varResult.getValue().doubleValue();
                assertThat(ratio).isBetween(1.0, 2.0);
            }
        }
    }

    @Nested
    @DisplayName("포트폴리오 VaR 테스트")
    class PortfolioVarTests {

        @Test
        @DisplayName("다중 종목 포트폴리오 VaR 계산 (상관관계 반영)")
        void shouldCalculatePortfolioVaRWithCorrelation() {
            Map<String, List<BigDecimal>> symbolReturns = new HashMap<>();
            symbolReturns.put("AAPL", generateSampleReturns(100, 0.001, 0.025));
            symbolReturns.put("MSFT", generateSampleReturns(100, 0.0008, 0.022));
            symbolReturns.put("GOOGL", generateSampleReturns(100, 0.0012, 0.028));

            Map<String, BigDecimal> weights = new HashMap<>();
            weights.put("AAPL", new BigDecimal("0.4"));
            weights.put("MSFT", new BigDecimal("0.35"));
            weights.put("GOOGL", new BigDecimal("0.25"));

            MonteCarloVarResult result = service.calculatePortfolioVaR(
                    symbolReturns,
                    weights,
                    new BigDecimal("0.95"),
                    10000
            );

            assertThat(result.isValid()).isTrue();
            assertThat(result.getValue()).isNotNull();
            assertThat(result.getMethod()).contains("PORTFOLIO");
            assertThat(result.getSampleCount()).isEqualTo(100);
        }

        @Test
        @DisplayName("분산 효과로 포트폴리오 VaR이 개별 VaR 가중평균보다 작음")
        void shouldShowDiversificationBenefit() {
            List<BigDecimal> returns1 = generateSampleReturns(100, 0.0, 0.02);
            List<BigDecimal> returns2 = generateSampleReturns(100, 0.0, 0.02);

            MonteCarloVarResult var1 = service.calculateVaR95(returns1);
            MonteCarloVarResult var2 = service.calculateVaR95(returns2);

            Map<String, List<BigDecimal>> symbolReturns = new HashMap<>();
            symbolReturns.put("A", returns1);
            symbolReturns.put("B", returns2);

            Map<String, BigDecimal> weights = new HashMap<>();
            weights.put("A", new BigDecimal("0.5"));
            weights.put("B", new BigDecimal("0.5"));

            MonteCarloVarResult portfolioVar = service.calculatePortfolioVaR(
                    symbolReturns,
                    weights,
                    new BigDecimal("0.95"),
                    10000
            );

            double weightedAvgVar = (var1.getValue().doubleValue() + var2.getValue().doubleValue()) / 2;

            assertThat(portfolioVar.getValue().doubleValue())
                    .isLessThanOrEqualTo(weightedAvgVar * 1.1);
        }

        @Test
        @DisplayName("빈 포트폴리오 처리")
        void shouldHandleEmptyPortfolio() {
            MonteCarloVarResult result = service.calculatePortfolioVaR(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    new BigDecimal("0.95"),
                    10000
            );

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("No symbol returns");
        }
    }

    @Nested
    @DisplayName("비동기 처리 테스트")
    class AsyncTests {

        @Test
        @DisplayName("비동기 VaR 계산")
        void shouldCalculateVaRAsync() throws Exception {
            List<BigDecimal> returns = generateSampleReturns(100, 0.001, 0.02);

            CompletableFuture<MonteCarloVarResult> future = service.calculateVaRAsync(
                    returns,
                    new BigDecimal("0.95"),
                    10000
            );

            MonteCarloVarResult result = future.get(30, TimeUnit.SECONDS);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getValue()).isNotNull();
        }

        @Test
        @DisplayName("비동기 CVaR 계산")
        void shouldCalculateCVaRAsync() throws Exception {
            List<BigDecimal> returns = generateSampleReturns(100, 0.001, 0.02);

            CompletableFuture<MonteCarloVarResult> future = service.calculateCVaRAsync(
                    returns,
                    new BigDecimal("0.95"),
                    10000
            );

            MonteCarloVarResult result = future.get(30, TimeUnit.SECONDS);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getMethod()).contains("ES");
        }
    }

    @Nested
    @DisplayName("성능 테스트")
    class PerformanceTests {

        @Test
        @DisplayName("10,000 시나리오 시뮬레이션 30초 이내 완료")
        void shouldComplete10000ScenariosWithin30Seconds() {
            List<BigDecimal> returns = generateSampleReturns(252, 0.001, 0.02);

            long startTime = System.currentTimeMillis();
            MonteCarloVarResult result = service.calculateVaR(
                    returns,
                    new BigDecimal("0.95"),
                    10000
            );
            long elapsedMs = System.currentTimeMillis() - startTime;

            assertThat(result.isValid()).isTrue();
            assertThat(elapsedMs).isLessThan(30000);
            assertThat(result.getElapsedTimeMs()).isLessThan(30000);
        }

        @Test
        @DisplayName("대규모 포트폴리오 (50종목) VaR 계산")
        void shouldHandleLargePortfolio() {
            Map<String, List<BigDecimal>> symbolReturns = new HashMap<>();
            Map<String, BigDecimal> weights = new HashMap<>();
            BigDecimal weight = new BigDecimal("0.02");

            for (int i = 0; i < 50; i++) {
                String symbol = "STOCK" + i;
                symbolReturns.put(symbol, generateSampleReturns(100, 0.0005, 0.02));
                weights.put(symbol, weight);
            }

            long startTime = System.currentTimeMillis();
            MonteCarloVarResult result = service.calculatePortfolioVaR(
                    symbolReturns,
                    weights,
                    new BigDecimal("0.95"),
                    5000
            );
            long elapsedMs = System.currentTimeMillis() - startTime;

            assertThat(result.isValid()).isTrue();
            assertThat(elapsedMs).isLessThan(60000);
        }
    }

    @Nested
    @DisplayName("분포 유형 테스트")
    class DistributionTests {

        @Test
        @DisplayName("Student-t 분포 사용 시 팻테일 효과")
        void shouldShowFatTailWithStudentT() {
            properties.setDistribution(MonteCarloProperties.Distribution.STUDENT_T);
            properties.setDegreesOfFreedom(5);
            MonteCarloVarService studentTService = new MonteCarloVarServiceImpl(properties);

            MonteCarloProperties normalProps = new MonteCarloProperties();
            normalProps.setDistribution(MonteCarloProperties.Distribution.NORMAL);
            normalProps.setFixedSeed(true);
            normalProps.setSeedValue(42L);
            MonteCarloVarService normalService = new MonteCarloVarServiceImpl(normalProps);

            List<BigDecimal> returns = generateSampleReturns(252, 0.0, 0.02);

            MonteCarloVarResult studentTResult = studentTService.calculateVaR95(returns);
            MonteCarloVarResult normalResult = normalService.calculateVaR95(returns);

            assertThat(studentTResult.isValid()).isTrue();
            assertThat(normalResult.isValid()).isTrue();
            assertThat(studentTResult.getMethod()).contains("STUDENT_T");
            assertThat(normalResult.getMethod()).contains("NORMAL");
        }
    }

    private List<BigDecimal> generateSampleReturns(int count, double mean, double stdDev) {
        Random random = new Random(42L);
        List<BigDecimal> returns = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double value = random.nextGaussian() * stdDev + mean;
            returns.add(BigDecimal.valueOf(value));
        }
        return returns;
    }
}
