package com.investment.core.engine.execution;

import com.investment.config.FrictionCostProperties;
import com.investment.core.engine.execution.dto.TcaEstimateRequest;
import com.investment.core.engine.execution.dto.TcaReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionCostAnalyzer 단위 테스트")
class TransactionCostAnalyzerTest {

    private TransactionCostAnalyzer analyzer;
    private FrictionCostProperties properties;

    @BeforeEach
    void setUp() {
        properties = new FrictionCostProperties();

        FrictionCostProperties.KoreaFees koreaFees = new FrictionCostProperties.KoreaFees();
        FrictionCostProperties.StockFee krStock = new FrictionCostProperties.StockFee();
        krStock.setCommission(new BigDecimal("0.000140527"));
        krStock.setTax(new BigDecimal("0.0018"));
        krStock.setSlippage(new BigDecimal("0.001"));
        FrictionCostProperties.EtfFee krEtf = new FrictionCostProperties.EtfFee();
        krEtf.setCommission(new BigDecimal("0.000140527"));
        krEtf.setTax(BigDecimal.ZERO);
        krEtf.setSlippage(new BigDecimal("0.0005"));
        koreaFees.setStock(krStock);
        koreaFees.setEtf(krEtf);

        FrictionCostProperties.UsaFees usaFees = new FrictionCostProperties.UsaFees();
        FrictionCostProperties.UsaStockFee usStock = new FrictionCostProperties.UsaStockFee();
        usStock.setCommission(new BigDecimal("0.0025"));
        usStock.setSecFee(new BigDecimal("0.0000278"));
        usStock.setTafPerShareUsd(new BigDecimal("0.000166"));
        usStock.setSlippage(new BigDecimal("0.0005"));
        FrictionCostProperties.CurrencyFee currency = new FrictionCostProperties.CurrencyFee();
        currency.setExchangeRateSpread(new BigDecimal("0.001"));
        usaFees.setStock(usStock);
        usaFees.setCurrency(currency);

        properties.setKorea(koreaFees);
        properties.setUsa(usaFees);

        analyzer = new TransactionCostAnalyzerImpl(properties);
    }

    @Nested
    @DisplayName("사전 비용 예측 (Pre-Trade)")
    class PreTradeTests {

        @Test
        @DisplayName("한국 주식 매수 비용 예측")
        void shouldEstimateKrStockBuyCost() {
            TcaEstimateRequest request = TcaEstimateRequest.builder()
                    .symbol("005930")
                    .market("KR")
                    .assetType("STOCK")
                    .side("BUY")
                    .quantity(100)
                    .arrivalPrice(new BigDecimal("70000"))
                    .avgDailyVolume(10000000L)
                    .build();

            TcaReport report = analyzer.estimateCost(request);

            assertThat(report.isValid()).isTrue();
            assertThat(report.getAnalysisType()).isEqualTo(TcaReport.AnalysisType.PRE_TRADE);
            assertThat(report.getCommissionCost()).isNotNull();
            assertThat(report.getTaxCost()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(report.getTotalEstimatedCost()).isGreaterThan(BigDecimal.ZERO);
            assertThat(report.getTotalEstimatedPct()).isLessThan(new BigDecimal("1"));
        }

        @Test
        @DisplayName("한국 주식 매도 비용 예측 (거래세 포함)")
        void shouldEstimateKrStockSellCostWithTax() {
            TcaEstimateRequest request = TcaEstimateRequest.builder()
                    .symbol("005930")
                    .market("KR")
                    .assetType("STOCK")
                    .side("SELL")
                    .quantity(100)
                    .arrivalPrice(new BigDecimal("70000"))
                    .build();

            TcaReport report = analyzer.estimateCost(request);

            assertThat(report.isValid()).isTrue();
            assertThat(report.getTaxCost()).isGreaterThan(BigDecimal.ZERO);
            assertThat(report.getTaxPct().doubleValue()).isGreaterThan(0.1);
        }

        @Test
        @DisplayName("미국 주식 매도 비용 예측 (SEC Fee + TAF)")
        void shouldEstimateUsStockSellCostWithSecFeeAndTaf() {
            TcaEstimateRequest request = TcaEstimateRequest.builder()
                    .symbol("AAPL")
                    .market("US")
                    .assetType("STOCK")
                    .side("SELL")
                    .quantity(50)
                    .arrivalPrice(new BigDecimal("180"))
                    .avgDailyVolume(50000000L)
                    .build();

            TcaReport report = analyzer.estimateCost(request);

            assertThat(report.isValid()).isTrue();
            assertThat(report.getTaxCost()).isGreaterThan(BigDecimal.ZERO);
            assertThat(report.getTafCost()).isGreaterThan(BigDecimal.ZERO);
            assertThat(report.getCommissionPct().doubleValue()).isCloseTo(0.25, org.assertj.core.data.Offset.offset(0.01));
        }

        @Test
        @DisplayName("한국 ETF 매도 비용 (거래세 면제)")
        void shouldEstimateKrEtfSellWithoutTax() {
            TcaEstimateRequest request = TcaEstimateRequest.builder()
                    .symbol("069500")
                    .market("KR")
                    .assetType("ETF")
                    .side("SELL")
                    .quantity(100)
                    .arrivalPrice(new BigDecimal("35000"))
                    .build();

            TcaReport report = analyzer.estimateCost(request);

            assertThat(report.isValid()).isTrue();
            assertThat(report.getTaxCost()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("시장 충격 비용 포함")
        void shouldIncludeMarketImpact() {
            TcaEstimateRequest request = TcaEstimateRequest.builder()
                    .symbol("005930")
                    .market("KR")
                    .assetType("STOCK")
                    .side("BUY")
                    .quantity(100000)
                    .arrivalPrice(new BigDecimal("70000"))
                    .avgDailyVolume(10000000L)
                    .build();

            TcaReport report = analyzer.estimateCost(request);

            assertThat(report.isValid()).isTrue();
            assertThat(report.getMarketImpactCost()).isGreaterThan(BigDecimal.ZERO);
            assertThat(report.getMarketImpactPct()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("잘못된 요청 처리")
        void shouldHandleInvalidRequest() {
            TcaEstimateRequest request = TcaEstimateRequest.builder()
                    .symbol("005930")
                    .quantity(0)
                    .arrivalPrice(new BigDecimal("70000"))
                    .build();

            TcaReport report = analyzer.estimateCost(request);

            assertThat(report.isValid()).isFalse();
            assertThat(report.getErrorMessage()).contains("Invalid");
        }
    }

    @Nested
    @DisplayName("사후 비용 분석 (Post-Trade)")
    class PostTradeTests {

        @Test
        @DisplayName("Implementation Shortfall 계산 - 매수 불리한 체결")
        void shouldCalculateIsForBuyUnfavorable() {
            TcaReport report = analyzer.analyzeCost(
                    "005930", "KR", "BUY",
                    100,
                    new BigDecimal("70000"),
                    new BigDecimal("70500"),
                    new BigDecimal("1000")
            );

            assertThat(report.isValid()).isTrue();
            assertThat(report.getAnalysisType()).isEqualTo(TcaReport.AnalysisType.POST_TRADE);
            assertThat(report.getImplementationShortfall()).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Implementation Shortfall 계산 - 매수 유리한 체결")
        void shouldCalculateIsForBuyFavorable() {
            TcaReport report = analyzer.analyzeCost(
                    "005930", "KR", "BUY",
                    100,
                    new BigDecimal("70000"),
                    new BigDecimal("69500"),
                    new BigDecimal("1000")
            );

            assertThat(report.isValid()).isTrue();
            BigDecimal priceImprovement = new BigDecimal("-50000");
            BigDecimal cost = new BigDecimal("1000");
            assertThat(report.getImplementationShortfall().doubleValue())
                    .isCloseTo(priceImprovement.add(cost).doubleValue(), org.assertj.core.data.Offset.offset(100.0));
        }

        @Test
        @DisplayName("Implementation Shortfall 계산 - 매도")
        void shouldCalculateIsForSell() {
            TcaReport report = analyzer.analyzeCost(
                    "005930", "KR", "SELL",
                    100,
                    new BigDecimal("70000"),
                    new BigDecimal("69500"),
                    new BigDecimal("1000")
            );

            assertThat(report.isValid()).isTrue();
            assertThat(report.getImplementationShortfall()).isGreaterThan(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("시장 충격 모델")
    class MarketImpactTests {

        @Test
        @DisplayName("참여율에 따른 시장 충격 증가")
        void shouldIncreaseImpactWithParticipationRate() {
            BigDecimal lowImpact = analyzer.estimateMarketImpact(1000, 10000000, null);
            BigDecimal highImpact = analyzer.estimateMarketImpact(100000, 10000000, null);

            assertThat(highImpact).isGreaterThan(lowImpact);
        }

        @Test
        @DisplayName("ADV 0일 때 영향 없음")
        void shouldReturnZeroWhenAdvIsZero() {
            BigDecimal impact = analyzer.estimateMarketImpact(1000, 0, null);

            assertThat(impact).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("왕복 비용")
    class RoundTripTests {

        @Test
        @DisplayName("한국 주식 왕복 비용")
        void shouldCalculateKrStockRoundTrip() {
            BigDecimal cost = analyzer.calculateRoundTripCost("KR", "STOCK", new BigDecimal("10000000"), 100);

            assertThat(cost).isGreaterThan(BigDecimal.ZERO);
            BigDecimal notional = new BigDecimal("10000000");
            BigDecimal pct = cost.divide(notional, 6, java.math.RoundingMode.HALF_UP);
            assertThat(pct.doubleValue()).isBetween(0.002, 0.005);
        }

        @Test
        @DisplayName("미국 주식 왕복 비용 (환전 포함)")
        void shouldCalculateUsStockRoundTripWithCurrency() {
            BigDecimal cost = analyzer.calculateRoundTripCost("US", "STOCK", new BigDecimal("10000"), 50);

            assertThat(cost).isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("한국 ETF 왕복 비용 (거래세 없음)")
        void shouldCalculateKrEtfRoundTrip() {
            BigDecimal stockCost = analyzer.calculateRoundTripCost("KR", "STOCK", new BigDecimal("10000000"), 100);
            BigDecimal etfCost = analyzer.calculateRoundTripCost("KR", "ETF", new BigDecimal("10000000"), 100);

            assertThat(etfCost).isLessThan(stockCost);
        }
    }
}
