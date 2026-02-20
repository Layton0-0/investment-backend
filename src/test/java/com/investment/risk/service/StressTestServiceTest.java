package com.investment.risk.service;

import com.investment.risk.dto.StressScenario;
import com.investment.risk.dto.StressTestResult;
import com.investment.risk.dto.StressTestResult.RiskGrade;
import com.investment.risk.service.StressTestService.PortfolioPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("StressTestService 단위 테스트")
class StressTestServiceTest {

    private StressTestService stressTestService;

    @BeforeEach
    void setUp() {
        stressTestService = new StressTestServiceImpl();
    }

    @Nested
    @DisplayName("기본 시나리오 테스트")
    class BasicScenarioTests {

        @Test
        @DisplayName("2008 금융위기 시나리오 - 손실률 -30%~-50% 범위")
        void shouldCalculateFinancialCrisis2008WithReasonableLoss() {
            Map<String, PortfolioPosition> portfolio = createEquityPortfolio();

            StressTestResult result = stressTestService.runStressTest(portfolio, "FINANCIAL_CRISIS_2008");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getScenarioCode()).isEqualTo("FINANCIAL_CRISIS_2008");
            assertThat(result.getScenarioName()).isEqualTo("2008 금융위기");

            BigDecimal lossPct = result.getPortfolioLossPct();
            assertThat(lossPct).isBetween(new BigDecimal("-60"), new BigDecimal("-30"));

            assertThat(result.getRiskGrade()).isIn(RiskGrade.CRITICAL, RiskGrade.HIGH);
            assertThat(result.getAppliedVixLevel()).isEqualByComparingTo(new BigDecimal("80"));
        }

        @Test
        @DisplayName("2020 코로나 폭락 시나리오")
        void shouldCalculateCovidCrash2020() {
            Map<String, PortfolioPosition> portfolio = createEquityPortfolio();

            StressTestResult result = stressTestService.runStressTest(portfolio, "COVID_CRASH_2020");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getScenarioCode()).isEqualTo("COVID_CRASH_2020");

            BigDecimal lossPct = result.getPortfolioLossPct();
            assertThat(lossPct).isBetween(new BigDecimal("-45"), new BigDecimal("-25"));

            assertThat(result.getSymbolImpacts()).isNotEmpty();
        }

        @Test
        @DisplayName("2022 금리 인상기 시나리오")
        void shouldCalculateRateHike2022() {
            Map<String, PortfolioPosition> portfolio = createMixedPortfolio();

            StressTestResult result = stressTestService.runStressTest(portfolio, "RATE_HIKE_2022");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getScenarioCode()).isEqualTo("RATE_HIKE_2022");

            assertThat(result.getAssetClassImpacts()).containsKey("EQUITY");
            assertThat(result.getAssetClassImpacts()).containsKey("BOND");
        }

        @Test
        @DisplayName("1987 블랙 먼데이 시나리오")
        void shouldCalculateBlackMonday1987() {
            Map<String, PortfolioPosition> portfolio = createEquityPortfolio();

            StressTestResult result = stressTestService.runStressTest(portfolio, "BLACK_MONDAY_1987");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getAppliedVixLevel()).isEqualByComparingTo(new BigDecimal("150"));
        }
    }

    @Nested
    @DisplayName("전체 시나리오 배치 테스트")
    class BatchTests {

        @Test
        @DisplayName("전체 시나리오 배치 실행 - 최소 3개 시나리오")
        void shouldRunAllScenarios() {
            Map<String, PortfolioPosition> portfolio = createEquityPortfolio();

            List<StressTestResult> results = stressTestService.runAllStressTests(portfolio);

            assertThat(results).hasSizeGreaterThanOrEqualTo(3);
            assertThat(results).allMatch(StressTestResult::isValid);

            List<String> scenarioCodes = results.stream()
                    .map(StressTestResult::getScenarioCode)
                    .toList();
            assertThat(scenarioCodes).contains(
                    "FINANCIAL_CRISIS_2008",
                    "COVID_CRASH_2020",
                    "RATE_HIKE_2022"
            );
        }

        @Test
        @DisplayName("배치 실행 시간 5분 이내")
        void shouldCompleteWithin5Minutes() {
            Map<String, PortfolioPosition> portfolio = createLargePortfolio(100);

            long startTime = System.currentTimeMillis();
            List<StressTestResult> results = stressTestService.runAllStressTests(portfolio);
            long elapsed = System.currentTimeMillis() - startTime;

            assertThat(elapsed).isLessThan(5 * 60 * 1000L);
            assertThat(results).isNotEmpty();

            System.out.println("Batch execution time: " + elapsed + "ms for " + results.size() + " scenarios");
        }
    }

    @Nested
    @DisplayName("사용자 정의 시나리오 테스트")
    class CustomScenarioTests {

        @Test
        @DisplayName("사용자 정의 시나리오 생성 및 실행")
        void shouldCreateAndRunCustomScenario() {
            Map<String, BigDecimal> customShocks = Map.of(
                    "EQUITY", new BigDecimal("-0.30"),
                    "BOND", new BigDecimal("-0.10"),
                    "COMMODITY", new BigDecimal("-0.20")
            );

            StressScenario custom = stressTestService.createCustomScenario(
                    "CUSTOM_CRISIS",
                    "커스텀 위기",
                    "사용자 정의 테스트 시나리오",
                    customShocks
            );

            assertThat(custom.getCode()).isEqualTo("CUSTOM_CRISIS");

            Map<String, PortfolioPosition> portfolio = createEquityPortfolio();
            StressTestResult result = stressTestService.runStressTest(portfolio, "CUSTOM_CRISIS");

            assertThat(result.isValid()).isTrue();
            assertThat(result.getPortfolioLossPct()).isEqualByComparingTo(new BigDecimal("-30.0000"));
        }

        @Test
        @DisplayName("생성된 커스텀 시나리오 목록에 추가됨")
        void shouldAddCustomScenarioToRegistry() {
            List<String> beforeCodes = stressTestService.getSupportedScenarioCodes();

            stressTestService.createCustomScenario(
                    "MY_SCENARIO",
                    "내 시나리오",
                    "테스트용",
                    Map.of("EQUITY", new BigDecimal("-0.15"))
            );

            List<String> afterCodes = stressTestService.getSupportedScenarioCodes();

            assertThat(afterCodes.size()).isEqualTo(beforeCodes.size() + 1);
            assertThat(afterCodes).contains("MY_SCENARIO");
        }
    }

    @Nested
    @DisplayName("자산군별 영향 테스트")
    class AssetClassImpactTests {

        @Test
        @DisplayName("채권 포함 포트폴리오 - 채권은 상승 or 소폭 하락")
        void shouldShowBondAsHedgeInCrisis() {
            Map<String, PortfolioPosition> portfolio = new HashMap<>();
            portfolio.put("SPY", new PortfolioPosition("SPY", "US", "EQUITY", new BigDecimal("50000")));
            portfolio.put("TLT", new PortfolioPosition("TLT", "US", "BOND", new BigDecimal("50000")));

            StressTestResult result = stressTestService.runStressTest(portfolio, "FINANCIAL_CRISIS_2008");

            assertThat(result.isValid()).isTrue();

            var equityImpact = result.getSymbolImpacts().stream()
                    .filter(i -> "SPY".equals(i.getSymbol()))
                    .findFirst()
                    .orElseThrow();
            var bondImpact = result.getSymbolImpacts().stream()
                    .filter(i -> "TLT".equals(i.getSymbol()))
                    .findFirst()
                    .orElseThrow();

            assertThat(equityImpact.getExpectedLossPct()).isLessThan(BigDecimal.ZERO);
            assertThat(bondImpact.getExpectedLossPct()).isGreaterThan(equityImpact.getExpectedLossPct());
        }

        @Test
        @DisplayName("개별 종목별 기여도 계산")
        void shouldCalculateContributionPerSymbol() {
            Map<String, PortfolioPosition> portfolio = new HashMap<>();
            portfolio.put("AAPL", new PortfolioPosition("AAPL", "US", "EQUITY", new BigDecimal("30000")));
            portfolio.put("MSFT", new PortfolioPosition("MSFT", "US", "EQUITY", new BigDecimal("20000")));
            portfolio.put("GOOGL", new PortfolioPosition("GOOGL", "US", "EQUITY", new BigDecimal("50000")));

            StressTestResult result = stressTestService.runStressTest(portfolio, "COVID_CRASH_2020");

            assertThat(result.isValid()).isTrue();

            BigDecimal totalContribution = result.getSymbolImpacts().stream()
                    .map(i -> i.getContributionToPortfolioLoss())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(totalContribution.abs().subtract(result.getPortfolioLossPct().abs()).abs())
                    .isLessThan(new BigDecimal("0.01"));
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTests {

        @Test
        @DisplayName("빈 포트폴리오 - insufficientData")
        void shouldHandleEmptyPortfolio() {
            Map<String, PortfolioPosition> portfolio = new HashMap<>();

            StressTestResult result = stressTestService.runStressTest(portfolio, "FINANCIAL_CRISIS_2008");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("데이터 부족");
        }

        @Test
        @DisplayName("존재하지 않는 시나리오")
        void shouldHandleUnknownScenario() {
            Map<String, PortfolioPosition> portfolio = createEquityPortfolio();

            StressTestResult result = stressTestService.runStressTest(portfolio, "UNKNOWN_SCENARIO");

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Unknown scenario");
        }
    }

    @Nested
    @DisplayName("시나리오 조회 테스트")
    class ScenarioQueryTests {

        @Test
        @DisplayName("지원 시나리오 목록 조회")
        void shouldReturnSupportedScenarioCodes() {
            List<String> codes = stressTestService.getSupportedScenarioCodes();

            assertThat(codes).contains(
                    "FINANCIAL_CRISIS_2008",
                    "COVID_CRASH_2020",
                    "RATE_HIKE_2022",
                    "BLACK_MONDAY_1987"
            );
        }

        @Test
        @DisplayName("시나리오 상세 조회")
        void shouldReturnScenarioDetail() {
            StressScenario scenario = stressTestService.getScenario("FINANCIAL_CRISIS_2008");

            assertThat(scenario).isNotNull();
            assertThat(scenario.getName()).isEqualTo("2008 금융위기");
            assertThat(scenario.getAssetClassShocks()).containsKey("EQUITY");
            assertThat(scenario.getVixLevel()).isEqualByComparingTo(new BigDecimal("80"));
        }
    }

    private Map<String, PortfolioPosition> createEquityPortfolio() {
        Map<String, PortfolioPosition> portfolio = new HashMap<>();
        portfolio.put("AAPL", new PortfolioPosition("AAPL", "US", "EQUITY", new BigDecimal("30000")));
        portfolio.put("MSFT", new PortfolioPosition("MSFT", "US", "EQUITY", new BigDecimal("25000")));
        portfolio.put("GOOGL", new PortfolioPosition("GOOGL", "US", "EQUITY", new BigDecimal("20000")));
        portfolio.put("삼성전자", new PortfolioPosition("005930", "KR", "EQUITY", new BigDecimal("25000")));
        return portfolio;
    }

    private Map<String, PortfolioPosition> createMixedPortfolio() {
        Map<String, PortfolioPosition> portfolio = new HashMap<>();
        portfolio.put("SPY", new PortfolioPosition("SPY", "US", "EQUITY", new BigDecimal("40000")));
        portfolio.put("QQQ", new PortfolioPosition("QQQ", "US", "GROWTH_EQUITY", new BigDecimal("20000")));
        portfolio.put("TLT", new PortfolioPosition("TLT", "US", "BOND", new BigDecimal("20000")));
        portfolio.put("GLD", new PortfolioPosition("GLD", "US", "COMMODITY", new BigDecimal("10000")));
        portfolio.put("VNQ", new PortfolioPosition("VNQ", "US", "REAL_ESTATE", new BigDecimal("10000")));
        return portfolio;
    }

    private Map<String, PortfolioPosition> createLargePortfolio(int size) {
        Map<String, PortfolioPosition> portfolio = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String symbol = "STOCK_" + i;
            portfolio.put(symbol, new PortfolioPosition(
                    symbol,
                    i % 2 == 0 ? "US" : "KR",
                    "EQUITY",
                    new BigDecimal("10000")
            ));
        }
        return portfolio;
    }
}
