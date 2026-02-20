package com.investment.core.engine.execution.algo;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * POV (Percentage of Volume) 알고리즘.
 * 시장 참여율 기반: 실시간 거래량의 일정 비율만큼만 집행.
 */
@Component
public class PovExecutionAlgorithm implements ExecutionAlgorithm {

    private static final int DEFAULT_CHECK_INTERVAL_MINUTES = 5;
    private static final BigDecimal DEFAULT_PARTICIPATION_RATE = new BigDecimal("10");

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.POV;
    }

    @Override
    public List<SlicePlan> planSlices(AlgorithmicOrder order) {
        AlgorithmicOrder.AlgorithmParameters params = order.getParameters();
        if (params == null) {
            params = AlgorithmicOrder.AlgorithmParameters.defaultPov();
        }

        int totalQuantity = order.getTotalQuantity();
        int durationMinutes = params.getDurationMinutes();
        BigDecimal participationRate = params.getParticipationRate() != null
                ? params.getParticipationRate()
                : DEFAULT_PARTICIPATION_RATE;

        int numChecks = Math.max(1, durationMinutes / DEFAULT_CHECK_INTERVAL_MINUTES);

        int quantityPerCheck = totalQuantity / numChecks;
        int remainder = totalQuantity % numChecks;

        List<SlicePlan> plans = new ArrayList<>();

        for (int i = 0; i < numChecks; i++) {
            int sliceQty = quantityPerCheck + (i < remainder ? 1 : 0);
            long delayMillis = (long) i * DEFAULT_CHECK_INTERVAL_MINUTES * 60 * 1000;

            double targetVolumeRatio = participationRate.doubleValue() / 100.0;

            plans.add(new SlicePlan(i + 1, sliceQty, delayMillis, targetVolumeRatio));
        }

        return plans;
    }

    /**
     * 실제 시장 참여율 계산.
     *
     * @param executedQuantity 체결된 수량
     * @param marketVolume 해당 기간 시장 총 거래량
     * @return 참여율 (%)
     */
    public static BigDecimal calculateParticipationRate(int executedQuantity, long marketVolume) {
        if (marketVolume == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(executedQuantity)
                .divide(BigDecimal.valueOf(marketVolume), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 다음 슬라이스 수량 계산 (실시간 거래량 기반).
     *
     * @param remainingQuantity 남은 수량
     * @param realtimeVolume 실시간 거래량
     * @param targetParticipationPct 목표 참여율 (%)
     * @param minQuantity 최소 주문 수량
     * @return 다음 슬라이스 수량
     */
    public static int calculateNextSliceQuantity(
            int remainingQuantity,
            long realtimeVolume,
            BigDecimal targetParticipationPct,
            int minQuantity) {

        if (remainingQuantity <= 0) {
            return 0;
        }

        double targetRatio = targetParticipationPct.doubleValue() / 100.0;
        int targetQuantity = (int) Math.round(realtimeVolume * targetRatio);

        targetQuantity = Math.max(targetQuantity, minQuantity);
        targetQuantity = Math.min(targetQuantity, remainingQuantity);

        return targetQuantity;
    }
}
