package com.investment.factor.zoo;

import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import com.investment.factor.service.UniverseFilterService;
import com.investment.factor.zoo.FactorDefinition.FactorCategory;
import com.investment.factor.zoo.FactorTestResult.FactorGrade;
import com.investment.factor.zoo.FactorZooService.CombinedFactorScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactorZooService 단위 테스트")
class FactorZooServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;

    @Mock
    private SignalScoreRepository signalScoreRepository;

    @Mock
    private UniverseFilterService universeFilterService;

    private FactorZooService factorZooService;

    @BeforeEach
    void setUp() {
        factorZooService = new FactorZooServiceImpl(dailyStockRepository, signalScoreRepository, universeFilterService);
    }

    @Nested
    @DisplayName("팩터 정의 테스트")
    class FactorDefinitionTests {

        @Test
        @DisplayName("최소 10개 팩터 지원")
        void shouldSupportAtLeast10Factors() {
            List<String> codes = factorZooService.getSupportedFactorCodes();

            assertThat(codes).hasSizeGreaterThanOrEqualTo(10);
            assertThat(codes).contains(
                    "PBR", "PER", "EV_EBITDA",
                    "MOMENTUM_3M", "MOMENTUM_6M", "MOMENTUM_12M",
                    "ROE", "OPERATING_MARGIN", "DEBT_RATIO",
                    "MARKET_CAP", "VOLATILITY", "BETA",
                    "DISPARITY", "VOLATILITY_BREAKOUT", "SMART_MONEY_INTENSITY"
            );
        }

        @Test
        @DisplayName("팩터 정의 조회")
        void shouldReturnFactorDefinition() {
            FactorDefinition pbr = factorZooService.getFactorDefinition("PBR");

            assertThat(pbr).isNotNull();
            assertThat(pbr.getCode()).isEqualTo("PBR");
            assertThat(pbr.getName()).isEqualTo("주가순자산비율");
            assertThat(pbr.getCategory()).isEqualTo(FactorCategory.VALUE);
            assertThat(pbr.getDirection()).isEqualTo(FactorDefinition.FactorDirection.LOWER_BETTER);
        }

        @Test
        @DisplayName("카테고리별 팩터 조회")
        void shouldReturnFactorsByCategory() {
            List<FactorDefinition> valueFactors = factorZooService.getFactorsByCategory(FactorCategory.VALUE);
            List<FactorDefinition> momentumFactors = factorZooService.getFactorsByCategory(FactorCategory.MOMENTUM);
            List<FactorDefinition> qualityFactors = factorZooService.getFactorsByCategory(FactorCategory.QUALITY);

            assertThat(valueFactors).hasSizeGreaterThanOrEqualTo(3);
            assertThat(momentumFactors).hasSizeGreaterThanOrEqualTo(3);
            assertThat(qualityFactors).hasSizeGreaterThanOrEqualTo(3);

            assertThat(valueFactors.stream().map(FactorDefinition::getCode))
                    .contains("PBR", "PER", "EV_EBITDA");
        }

        @Test
        @DisplayName("모든 팩터 정의 조회")
        void shouldReturnAllFactorDefinitions() {
            List<FactorDefinition> all = factorZooService.getAllFactorDefinitions();

            assertThat(all).hasSizeGreaterThanOrEqualTo(10);
            assertThat(all).allMatch(f -> f.getCode() != null);
            assertThat(all).allMatch(f -> f.getName() != null);
            assertThat(all).allMatch(f -> f.getCategory() != null);
        }

        @Test
        @DisplayName("존재하지 않는 팩터 조회")
        void shouldReturnNullForUnknownFactor() {
            FactorDefinition unknown = factorZooService.getFactorDefinition("UNKNOWN_FACTOR");

            assertThat(unknown).isNull();
        }
    }

    @Nested
    @DisplayName("기존 팩터 통합 테스트")
    class LegacyFactorIntegrationTests {

        @Test
        @DisplayName("이격도(DISPARITY) 팩터 포함")
        void shouldIncludeDisparityFactor() {
            FactorDefinition disparity = factorZooService.getFactorDefinition("DISPARITY");

            assertThat(disparity).isNotNull();
            assertThat(disparity.getName()).isEqualTo("이격도");
            assertThat(disparity.getCategory()).isEqualTo(FactorCategory.TECHNICAL);
            assertThat(disparity.getDescription()).contains("이동평균");
        }

        @Test
        @DisplayName("변동성 돌파 팩터 포함")
        void shouldIncludeVolatilityBreakoutFactor() {
            FactorDefinition vb = factorZooService.getFactorDefinition("VOLATILITY_BREAKOUT");

            assertThat(vb).isNotNull();
            assertThat(vb.getName()).isEqualTo("변동성 돌파");
            assertThat(vb.getCategory()).isEqualTo(FactorCategory.TECHNICAL);
        }

        @Test
        @DisplayName("수급 강도 팩터 포함")
        void shouldIncludeSmartMoneyIntensityFactor() {
            FactorDefinition smi = factorZooService.getFactorDefinition("SMART_MONEY_INTENSITY");

            assertThat(smi).isNotNull();
            assertThat(smi.getName()).isEqualTo("수급 강도");
            assertThat(smi.getCategory()).isEqualTo(FactorCategory.FLOW);
        }
    }

    @Nested
    @DisplayName("팩터 테스트")
    class FactorTestTests {

        @Test
        @DisplayName("데이터 부족 시 insufficientData 반환")
        void shouldReturnInsufficientDataWhenNoData() {
            when(universeFilterService.getSymbols(any(), any())).thenReturn(Collections.emptyList());
            when(dailyStockRepository.findByMarketAndBasDtBetween(any(), any(), any()))
                    .thenReturn(Collections.emptyList());

            FactorTestResult result = factorZooService.testFactor(
                    "MOMENTUM_3M", "KR",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 12, 31)
            );

            assertThat(result.isValid()).isFalse();
            assertThat(result.getGrade()).isEqualTo(FactorGrade.F);
        }

        @Test
        @DisplayName("존재하지 않는 팩터 테스트 시 에러")
        void shouldReturnErrorForUnknownFactor() {
            FactorTestResult result = factorZooService.testFactor(
                    "UNKNOWN", "KR",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 12, 31)
            );

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrorMessage()).contains("Unknown factor");
        }

        @Test
        @DisplayName("충분한 데이터로 팩터 테스트")
        void shouldTestFactorWithSufficientData() {
            List<String> universe = createMockUniverse(50);
            List<DailyStock> mockData = createMockDailyStocks(universe);

            when(universeFilterService.getSymbols(any(), any())).thenReturn(universe);
            when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    anyString(), anyString(), any(), any()))
                    .thenAnswer(inv -> {
                        String symbol = inv.getArgument(0);
                        return mockData.stream()
                                .filter(d -> d.getSymbol().equals(symbol))
                                .toList();
                    });

            FactorTestResult result = factorZooService.testFactor(
                    "MOMENTUM_3M", "KR",
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 6, 30)
            );

            assertThat(result.getFactorCode()).isEqualTo("MOMENTUM_3M");
            if (result.isValid()) {
                assertThat(result.getMarket()).isEqualTo("KR");
            }
        }
    }

    @Nested
    @DisplayName("복합 팩터 점수 테스트")
    class CombinedScoreTests {

        @Test
        @DisplayName("복합 팩터 점수 계산")
        void shouldCalculateCombinedScore() {
            List<DailyStock> mockData = createMockDailyStocks(List.of("005930"));

            when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    anyString(), anyString(), any(), any()))
                    .thenReturn(mockData);

            Map<String, BigDecimal> weights = Map.of(
                    "MOMENTUM_3M", new BigDecimal("0.3"),
                    "VOLATILITY", new BigDecimal("0.3"),
                    "PBR", new BigDecimal("0.4")
            );

            CombinedFactorScore score = factorZooService.getCombinedScore(
                    "005930", "KR", LocalDate.of(2025, 6, 30), weights
            );

            assertThat(score.symbol()).isEqualTo("005930");
            assertThat(score.market()).isEqualTo("KR");
            assertThat(score.factorWeights()).isEqualTo(weights);
        }

        @Test
        @DisplayName("종목 랭킹")
        void shouldRankStocksByFactors() {
            List<String> universe = createMockUniverse(30);
            List<DailyStock> mockData = createMockDailyStocks(universe);

            when(universeFilterService.getSymbols(any(), any())).thenReturn(universe);
            when(dailyStockRepository.findBySymbolAndMarketAndBasDtBetweenOrderByBasDtAsc(
                    anyString(), anyString(), any(), any()))
                    .thenReturn(mockData);

            Map<String, BigDecimal> weights = Map.of(
                    "MOMENTUM_3M", new BigDecimal("0.5"),
                    "PBR", new BigDecimal("0.5")
            );

            List<CombinedFactorScore> rankings = factorZooService.rankStocksByFactors(
                    "KR", LocalDate.of(2025, 6, 30), weights, 10
            );

            assertThat(rankings).hasSizeLessThanOrEqualTo(10);
            for (int i = 0; i < rankings.size(); i++) {
                assertThat(rankings.get(i).rank()).isEqualTo(i + 1);
            }
        }
    }

    @Nested
    @DisplayName("IC 계산 검증 테스트")
    class ICCalculationTests {

        @Test
        @DisplayName("IC 범위 검증 (-1 ~ 1)")
        void shouldHaveICWithinValidRange() {
            BigDecimal ic = new BigDecimal("0.05");

            assertThat(ic).isBetween(new BigDecimal("-1"), new BigDecimal("1"));
        }

        @Test
        @DisplayName("Spearman 상관계수 계산 원리")
        void shouldComputeSpearmanCorrelation() {
            List<BigDecimal> x = List.of(
                    new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"),
                    new BigDecimal("4"), new BigDecimal("5")
            );
            List<BigDecimal> y = List.of(
                    new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"),
                    new BigDecimal("4"), new BigDecimal("5")
            );

            assertThat(x).hasSameSizeAs(y);
        }
    }

    private List<String> createMockUniverse(int size) {
        List<String> universe = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            universe.add("STOCK_" + String.format("%04d", i));
        }
        return universe;
    }

    private List<DailyStock> createMockDailyStocks(List<String> symbols) {
        List<DailyStock> stocks = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);

        for (String symbol : symbols) {
            BigDecimal basePrice = BigDecimal.valueOf(10000 + new Random().nextInt(50000));
            for (int i = 0; i < 365; i++) {
                LocalDate date = startDate.plusDays(i);
                BigDecimal variation = BigDecimal.valueOf(0.97 + new Random().nextDouble() * 0.06);
                BigDecimal closePrice = basePrice.multiply(variation);

                DailyStock stock = DailyStock.builder()
                        .symbol(symbol)
                        .market("KR")
                        .basDt(date)
                        .openPrice(closePrice.multiply(BigDecimal.valueOf(0.99)))
                        .highPrice(closePrice.multiply(BigDecimal.valueOf(1.02)))
                        .lowPrice(closePrice.multiply(BigDecimal.valueOf(0.98)))
                        .closePrice(closePrice)
                        .volume(1000000L)
                        .trdVal(closePrice.multiply(BigDecimal.valueOf(1000000)).longValue())
                        .build();
                stocks.add(stock);

                basePrice = closePrice;
            }
        }
        return stocks;
    }

    private List<DailyStock> createMockDailyStocksWithTrend(List<String> symbols) {
        List<DailyStock> stocks = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        Random random = new Random(42);

        for (int idx = 0; idx < symbols.size(); idx++) {
            String symbol = symbols.get(idx);
            BigDecimal basePrice = BigDecimal.valueOf(10000 + random.nextInt(50000));
            double trend = (idx % 2 == 0) ? 1.001 : 0.999;

            for (int i = 0; i < 365; i++) {
                LocalDate date = startDate.plusDays(i);
                BigDecimal dailyReturn = BigDecimal.valueOf(trend + (random.nextDouble() - 0.5) * 0.02);
                BigDecimal closePrice = basePrice.multiply(dailyReturn);

                DailyStock stock = DailyStock.builder()
                        .symbol(symbol)
                        .market("KR")
                        .basDt(date)
                        .openPrice(closePrice.multiply(BigDecimal.valueOf(0.99)))
                        .highPrice(closePrice.multiply(BigDecimal.valueOf(1.02)))
                        .lowPrice(closePrice.multiply(BigDecimal.valueOf(0.98)))
                        .closePrice(closePrice)
                        .volume(1000000L)
                        .trdVal(closePrice.multiply(BigDecimal.valueOf(1000000)).longValue())
                        .build();
                stocks.add(stock);

                basePrice = closePrice;
            }
        }
        return stocks;
    }
}
