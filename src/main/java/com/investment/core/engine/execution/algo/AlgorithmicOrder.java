package com.investment.core.engine.execution.algo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * 알고리즘 주문 정의.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgorithmicOrder {

    private String orderId;
    private String symbol;
    private String market;
    private OrderSide side;
    private int totalQuantity;
    private BigDecimal limitPrice;

    private AlgorithmType algorithm;
    private AlgorithmParameters parameters;

    public enum OrderSide {
        BUY,
        SELL
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlgorithmParameters {

        /**
         * 전체 집행 시간 (분). 기본 60분.
         */
        @Builder.Default
        private int durationMinutes = 60;

        /**
         * 슬라이스 개수. TWAP에서 사용.
         */
        @Builder.Default
        private int slices = 12;

        /**
         * 슬라이스 간 간격 (분). 기본 5분.
         */
        @Builder.Default
        private int intervalMinutes = 5;

        /**
         * 시장 참여율 (%). POV에서 사용.
         */
        @Builder.Default
        private BigDecimal participationRate = new BigDecimal("10");

        /**
         * 집행 시작 시간.
         */
        private LocalTime startTime;

        /**
         * 집행 종료 시간.
         */
        private LocalTime endTime;

        /**
         * 가격 제한 (%). 이 범위를 벗어나면 집행 중단.
         */
        @Builder.Default
        private BigDecimal priceLimitPct = new BigDecimal("2");

        /**
         * 최소 슬라이스 수량.
         */
        @Builder.Default
        private int minSliceQuantity = 1;

        /**
         * 미체결 시 재시도 여부.
         */
        @Builder.Default
        private boolean retryOnUnfilled = true;

        /**
         * 최대 재시도 횟수.
         */
        @Builder.Default
        private int maxRetries = 3;

        public static AlgorithmParameters defaultTwap() {
            return AlgorithmParameters.builder()
                    .durationMinutes(60)
                    .slices(12)
                    .intervalMinutes(5)
                    .build();
        }

        public static AlgorithmParameters defaultVwap() {
            return AlgorithmParameters.builder()
                    .durationMinutes(60)
                    .slices(12)
                    .intervalMinutes(5)
                    .startTime(LocalTime.of(9, 0))
                    .endTime(LocalTime.of(15, 30))
                    .build();
        }

        public static AlgorithmParameters defaultPov() {
            return AlgorithmParameters.builder()
                    .participationRate(new BigDecimal("10"))
                    .durationMinutes(120)
                    .build();
        }
    }
}
