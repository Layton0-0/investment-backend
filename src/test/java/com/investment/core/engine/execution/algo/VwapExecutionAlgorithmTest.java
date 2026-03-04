package com.investment.core.engine.execution.algo;

import com.investment.core.engine.execution.algo.ExecutionAlgorithm.SlicePlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VwapExecutionAlgorithm")
class VwapExecutionAlgorithmTest {

    private final VwapExecutionAlgorithm algorithm = new VwapExecutionAlgorithm();

    @Test
    @DisplayName("getType은 VWAP 반환")
    void getType_returnsVwap() {
        assertThat(algorithm.getType()).isEqualTo(AlgorithmType.VWAP);
    }

    @Test
    @DisplayName("KR 시장 U자형 거래량 프로파일 - 첫·끝 슬라이스가 중간보다 비중 큼")
    void planSlices_krMarket_uShapeVolumeProfile() {
        AlgorithmicOrder order = AlgorithmicOrder.builder()
                .orderId("VWAP-KR-1")
                .symbol("005930")
                .market("KR")
                .totalQuantity(1000)
                .algorithm(AlgorithmType.VWAP)
                .parameters(AlgorithmicOrder.AlgorithmParameters.builder()
                        .slices(13)
                        .intervalMinutes(5)
                        .build())
                .build();

        List<SlicePlan> plans = algorithm.planSlices(order);

        assertThat(plans).hasSize(13);
        int totalQty = plans.stream().mapToInt(SlicePlan::quantity).sum();
        assertThat(totalQty).isEqualTo(1000);

        // U자형: 첫 구간(개장 직후)과 끝 구간(장 마감) 비중이 중간(점심)보다 커야 함
        double firstRatio = plans.get(0).targetVolumeRatio();
        double midRatio = plans.get(6).targetVolumeRatio();   // 12:00 전후
        double lastRatio = plans.get(12).targetVolumeRatio();
        assertThat(firstRatio).isGreaterThan(midRatio);
        assertThat(lastRatio).isGreaterThan(midRatio);
    }

    @Test
    @DisplayName("US 시장 U자형 거래량 프로파일 - 첫 슬라이스 수량이 중간보다 큼")
    void planSlices_usMarket_uShapeVolumeProfile() {
        AlgorithmicOrder order = AlgorithmicOrder.builder()
                .orderId("VWAP-US-1")
                .symbol("AAPL")
                .market("US")
                .totalQuantity(1200)
                .algorithm(AlgorithmType.VWAP)
                .parameters(AlgorithmicOrder.AlgorithmParameters.builder()
                        .slices(12)
                        .intervalMinutes(5)
                        .build())
                .build();

        List<SlicePlan> plans = algorithm.planSlices(order);

        assertThat(plans).hasSize(12);
        int totalQty = plans.stream().mapToInt(SlicePlan::quantity).sum();
        assertThat(totalQty).isEqualTo(1200);
        assertThat(plans.get(0).quantity()).isGreaterThan(plans.get(6).quantity());
    }

    @Test
    @DisplayName("calculateVwap - 가격·수량 리스트로 VWAP 계산")
    void calculateVwap_returnsVolumeWeightedAverage() {
        List<BigDecimal> prices = List.of(
                new BigDecimal("100"), new BigDecimal("102"), new BigDecimal("98")
        );
        List<Integer> quantities = List.of(100, 200, 100);

        BigDecimal vwap = VwapExecutionAlgorithm.calculateVwap(prices, quantities);

        // (100*100 + 102*200 + 98*100) / 400 = 40200/400 = 100.5
        assertThat(vwap).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    @Test
    @DisplayName("calculateSlippage - 실제 VWAP vs 시장 VWAP 괴리율")
    void calculateSlippage_returnsPercentDeviation() {
        BigDecimal actualVwap = new BigDecimal("101.00");
        BigDecimal marketVwap = new BigDecimal("100.00");

        BigDecimal slippage = VwapExecutionAlgorithm.calculateSlippage(actualVwap, marketVwap);

        assertThat(slippage).isEqualByComparingTo(new BigDecimal("1.00"));
    }
}
