package com.investment.core.engine.execution.algo;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TWAP (Time-Weighted Average Price) 알고리즘.
 * 시간 균등 분할: 전체 수량을 N개 슬라이스로 나눠 일정 시간 간격으로 집행.
 */
@Component
public class TwapExecutionAlgorithm implements ExecutionAlgorithm {

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.TWAP;
    }

    @Override
    public List<SlicePlan> planSlices(AlgorithmicOrder order) {
        AlgorithmicOrder.AlgorithmParameters params = order.getParameters();
        if (params == null) {
            params = AlgorithmicOrder.AlgorithmParameters.defaultTwap();
        }

        int totalQuantity = order.getTotalQuantity();
        int slices = Math.max(1, params.getSlices());
        int intervalMinutes = params.getIntervalMinutes();
        int minSliceQuantity = params.getMinSliceQuantity();

        if (totalQuantity < slices * minSliceQuantity) {
            slices = Math.max(1, totalQuantity / minSliceQuantity);
        }

        int baseQuantityPerSlice = totalQuantity / slices;
        int remainder = totalQuantity % slices;

        List<SlicePlan> plans = new ArrayList<>();
        int cumulativeQuantity = 0;

        for (int i = 0; i < slices; i++) {
            int sliceQty = baseQuantityPerSlice + (i < remainder ? 1 : 0);
            cumulativeQuantity += sliceQty;

            long delayMillis = (long) i * intervalMinutes * 60 * 1000;
            double targetVolumeRatio = 1.0 / slices;

            plans.add(new SlicePlan(i + 1, sliceQty, delayMillis, targetVolumeRatio));
        }

        return plans;
    }

    /**
     * TWAP 분할 오차 계산 (%).
     *
     * @param plans 슬라이스 계획
     * @param totalQuantity 전체 수량
     * @return 최대 분할 오차 (%)
     */
    public static double calculateSplitDeviation(List<SlicePlan> plans, int totalQuantity) {
        if (plans.isEmpty() || totalQuantity == 0) {
            return 0;
        }

        double idealQuantityPerSlice = (double) totalQuantity / plans.size();
        double maxDeviation = 0;

        for (SlicePlan plan : plans) {
            double deviation = Math.abs(plan.quantity() - idealQuantityPerSlice) / idealQuantityPerSlice * 100;
            maxDeviation = Math.max(maxDeviation, deviation);
        }

        return maxDeviation;
    }
}
