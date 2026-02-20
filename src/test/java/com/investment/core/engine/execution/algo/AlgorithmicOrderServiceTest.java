package com.investment.core.engine.execution.algo;

import com.investment.core.engine.execution.ExecutionGateway;
import com.investment.core.engine.execution.algo.AlgorithmicExecutionResult.ExecutionStatus;
import com.investment.core.engine.execution.algo.AlgorithmicOrder.AlgorithmParameters;
import com.investment.core.engine.execution.algo.AlgorithmicOrder.OrderSide;
import com.investment.core.engine.execution.algo.ExecutionAlgorithm.SlicePlan;
import com.investment.order.dto.OrderResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlgorithmicOrderService 단위 테스트")
class AlgorithmicOrderServiceTest {

    @Mock
    private ExecutionGateway executionGateway;

    @Mock
    private TaskScheduler taskScheduler;

    private TwapExecutionAlgorithm twapAlgorithm;
    private VwapExecutionAlgorithm vwapAlgorithm;
    private PovExecutionAlgorithm povAlgorithm;

    private AlgorithmicOrderService service;

    @BeforeEach
    void setUp() {
        twapAlgorithm = new TwapExecutionAlgorithm();
        vwapAlgorithm = new VwapExecutionAlgorithm();
        povAlgorithm = new PovExecutionAlgorithm();

        service = new AlgorithmicOrderServiceImpl(
                executionGateway,
                taskScheduler,
                twapAlgorithm,
                vwapAlgorithm,
                povAlgorithm
        );
    }

    @Nested
    @DisplayName("TWAP 알고리즘 테스트")
    class TwapTests {

        @Test
        @DisplayName("TWAP 시간 균등 분할 오차 5% 이내")
        void shouldSplitEvenlyWithin5Percent() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-001")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(1000)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.builder()
                            .slices(10)
                            .intervalMinutes(5)
                            .build())
                    .build();

            List<SlicePlan> plans = twapAlgorithm.planSlices(order);

            assertThat(plans).hasSize(10);

            int totalPlanned = plans.stream().mapToInt(SlicePlan::quantity).sum();
            assertThat(totalPlanned).isEqualTo(1000);

            double deviation = TwapExecutionAlgorithm.calculateSplitDeviation(plans, 1000);
            assertThat(deviation).isLessThan(5.0);
        }

        @Test
        @DisplayName("TWAP 소량 주문 처리")
        void shouldHandleSmallQuantity() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-002")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(5)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.builder()
                            .slices(12)
                            .minSliceQuantity(1)
                            .build())
                    .build();

            List<SlicePlan> plans = twapAlgorithm.planSlices(order);

            int totalPlanned = plans.stream().mapToInt(SlicePlan::quantity).sum();
            assertThat(totalPlanned).isEqualTo(5);
            assertThat(plans).hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("TWAP 실행 시작")
        void shouldStartTwapExecution() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-003")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(100)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.defaultTwap())
                    .build();

            AlgorithmicExecutionResult result = service.execute(order, "user1");

            assertThat(result).isNotNull();
            assertThat(result.getExecutionId()).isNotBlank();
            assertThat(result.getSymbol()).isEqualTo("005930");
            assertThat(result.getAlgorithm()).isEqualTo(AlgorithmType.TWAP);
            assertThat(result.getStatus()).isIn(ExecutionStatus.PENDING, ExecutionStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("VWAP 알고리즘 테스트")
    class VwapTests {

        @Test
        @DisplayName("VWAP 거래량 가중 분할")
        void shouldSplitByVolumeProfile() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-004")
                    .symbol("AAPL")
                    .market("US")
                    .side(OrderSide.SELL)
                    .totalQuantity(1000)
                    .algorithm(AlgorithmType.VWAP)
                    .parameters(AlgorithmParameters.builder()
                            .slices(12)
                            .intervalMinutes(5)
                            .build())
                    .build();

            List<SlicePlan> plans = vwapAlgorithm.planSlices(order);

            assertThat(plans).hasSize(12);

            int totalPlanned = plans.stream().mapToInt(SlicePlan::quantity).sum();
            assertThat(totalPlanned).isEqualTo(1000);

            assertThat(plans.get(0).quantity()).isGreaterThan(plans.get(6).quantity());
        }

        @Test
        @DisplayName("VWAP 계산 정확도")
        void shouldCalculateVwapCorrectly() {
            List<BigDecimal> prices = List.of(
                    new BigDecimal("100"), new BigDecimal("101"), new BigDecimal("99")
            );
            List<Integer> quantities = List.of(100, 200, 100);

            BigDecimal vwap = VwapExecutionAlgorithm.calculateVwap(prices, quantities);

            BigDecimal expected = new BigDecimal("100.25");
            assertThat(vwap).isEqualByComparingTo(expected);
        }

        @Test
        @DisplayName("VWAP 괴리율 계산")
        void shouldCalculateSlippage() {
            BigDecimal actualVwap = new BigDecimal("100.50");
            BigDecimal marketVwap = new BigDecimal("100.00");

            BigDecimal slippage = VwapExecutionAlgorithm.calculateSlippage(actualVwap, marketVwap);

            assertThat(slippage).isEqualByComparingTo(new BigDecimal("0.50"));
        }

        @Test
        @DisplayName("VWAP 괴리율 10% 이내 검증")
        void shouldVerifySlippageWithin10Percent() {
            BigDecimal actualVwap = new BigDecimal("105.00");
            BigDecimal marketVwap = new BigDecimal("100.00");

            BigDecimal slippage = VwapExecutionAlgorithm.calculateSlippage(actualVwap, marketVwap);

            assertThat(slippage.abs()).isLessThan(new BigDecimal("10"));
        }
    }

    @Nested
    @DisplayName("POV 알고리즘 테스트")
    class PovTests {

        @Test
        @DisplayName("POV 참여율 기반 분할")
        void shouldSplitByParticipationRate() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-005")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(500)
                    .algorithm(AlgorithmType.POV)
                    .parameters(AlgorithmParameters.builder()
                            .participationRate(new BigDecimal("10"))
                            .durationMinutes(60)
                            .build())
                    .build();

            List<SlicePlan> plans = povAlgorithm.planSlices(order);

            int totalPlanned = plans.stream().mapToInt(SlicePlan::quantity).sum();
            assertThat(totalPlanned).isEqualTo(500);

            assertThat(plans).isNotEmpty();
        }

        @Test
        @DisplayName("POV 다음 슬라이스 수량 계산")
        void shouldCalculateNextSliceQuantity() {
            int remaining = 500;
            long realtimeVolume = 10000;
            BigDecimal targetPct = new BigDecimal("10");
            int minQty = 10;

            int nextQty = PovExecutionAlgorithm.calculateNextSliceQuantity(
                    remaining, realtimeVolume, targetPct, minQty);

            assertThat(nextQty).isLessThanOrEqualTo(remaining);
            assertThat(nextQty).isGreaterThanOrEqualTo(minQty);

            assertThat(nextQty).isEqualTo(500);
        }

        @Test
        @DisplayName("POV 참여율 계산")
        void shouldCalculateParticipationRate() {
            int executed = 100;
            long marketVolume = 10000;

            BigDecimal rate = PovExecutionAlgorithm.calculateParticipationRate(executed, marketVolume);

            assertThat(rate).isEqualByComparingTo(new BigDecimal("1.00"));
        }
    }

    @Nested
    @DisplayName("주문 취소 및 재개 테스트")
    class CancelResumeTests {

        @Test
        @DisplayName("주문 취소 기능")
        void shouldCancelOrder() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-006")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(100)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.defaultTwap())
                    .build();

            AlgorithmicExecutionResult started = service.execute(order, "user1");
            String executionId = started.getExecutionId();

            AlgorithmicExecutionResult cancelled = service.cancel(executionId);

            assertThat(cancelled).isNotNull();
            assertThat(cancelled.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
        }

        @Test
        @DisplayName("주문 재개 기능")
        void shouldResumeOrder() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-007")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(100)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.defaultTwap())
                    .build();

            AlgorithmicExecutionResult started = service.execute(order, "user1");
            String executionId = started.getExecutionId();

            service.cancel(executionId);
            AlgorithmicExecutionResult resumed = service.resume(executionId);

            assertThat(resumed).isNotNull();
            assertThat(resumed.getStatus()).isEqualTo(ExecutionStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("진행 상태 조회")
        void shouldGetProgress() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-008")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(100)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.defaultTwap())
                    .build();

            AlgorithmicExecutionResult started = service.execute(order, "user1");
            String executionId = started.getExecutionId();

            AlgorithmicExecutionResult progress = service.getProgress(executionId);

            assertThat(progress).isNotNull();
            assertThat(progress.getExecutionId()).isEqualTo(executionId);
            assertThat(progress.getTotalQuantity()).isEqualTo(100);
        }
    }

    @Nested
    @DisplayName("슬라이스 미리보기 테스트")
    class PreviewTests {

        @Test
        @DisplayName("TWAP 슬라이스 미리보기")
        void shouldPreviewTwapSlices() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("PREVIEW-001")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(120)
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.builder()
                            .slices(12)
                            .intervalMinutes(5)
                            .build())
                    .build();

            List<SlicePlan> preview = service.previewSlices(order);

            assertThat(preview).hasSize(12);
            assertThat(preview.stream().mapToInt(SlicePlan::quantity).sum()).isEqualTo(120);

            for (int i = 0; i < preview.size(); i++) {
                assertThat(preview.get(i).delayMillis())
                        .isEqualTo((long) i * 5 * 60 * 1000);
            }
        }

        @Test
        @DisplayName("지원 알고리즘 목록 조회")
        void shouldReturnSupportedAlgorithms() {
            List<AlgorithmType> algorithms = service.getSupportedAlgorithms();

            assertThat(algorithms).contains(
                    AlgorithmType.TWAP,
                    AlgorithmType.VWAP,
                    AlgorithmType.POV
            );
        }
    }

    @Nested
    @DisplayName("ExecutionGateway 통합 테스트")
    class IntegrationTests {

        @Test
        @DisplayName("ExecutionGateway 통한 주문 실행 준비")
        void shouldPrepareExecutionViaGateway() {
            AlgorithmicOrder order = AlgorithmicOrder.builder()
                    .orderId("TEST-GATEWAY")
                    .symbol("005930")
                    .market("KR")
                    .side(OrderSide.BUY)
                    .totalQuantity(100)
                    .limitPrice(new BigDecimal("50000"))
                    .algorithm(AlgorithmType.TWAP)
                    .parameters(AlgorithmParameters.builder()
                            .slices(2)
                            .intervalMinutes(1)
                            .build())
                    .build();

            AlgorithmicExecutionResult result = service.execute(order, "user1");

            assertThat(result).isNotNull();
            assertThat(result.getAlgorithm()).isEqualTo(AlgorithmType.TWAP);
            assertThat(result.getTotalQuantity()).isEqualTo(100);
            assertThat(result.getStatus()).isIn(ExecutionStatus.PENDING, ExecutionStatus.IN_PROGRESS);
        }
    }
}
