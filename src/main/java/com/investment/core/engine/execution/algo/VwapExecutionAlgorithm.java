package com.investment.core.engine.execution.algo;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * VWAP (Volume-Weighted Average Price) 알고리즘.
 * 거래량 가중 분할: 과거 거래량 프로필에 따라 거래량이 많은 시간대에 더 많이 집행.
 */
@Component
public class VwapExecutionAlgorithm implements ExecutionAlgorithm {

    /**
     * 한국 시장 시간대별 거래량 비율 (9시~15시30분, 30분 간격).
     * 실제 운영 시 실시간 거래량 프로필로 대체해야 함.
     */
    private static final double[] KR_VOLUME_PROFILE = {
            0.12,  // 09:00-09:30 (개장 직후 높음)
            0.10,  // 09:30-10:00
            0.08,  // 10:00-10:30
            0.07,  // 10:30-11:00
            0.06,  // 11:00-11:30
            0.06,  // 11:30-12:00
            0.05,  // 12:00-12:30 (점심 낮음)
            0.05,  // 12:30-13:00
            0.06,  // 13:00-13:30
            0.07,  // 13:30-14:00
            0.08,  // 14:00-14:30
            0.10,  // 14:30-15:00
            0.10   // 15:00-15:30 (장 마감 직전 높음)
    };

    /**
     * 미국 시장 시간대별 거래량 비율 (9:30~16:00, 30분 간격).
     */
    private static final double[] US_VOLUME_PROFILE = {
            0.15,  // 09:30-10:00 (개장 직후 매우 높음)
            0.10,  // 10:00-10:30
            0.08,  // 10:30-11:00
            0.06,  // 11:00-11:30
            0.05,  // 11:30-12:00
            0.04,  // 12:00-12:30 (점심 낮음)
            0.04,  // 12:30-13:00
            0.05,  // 13:00-13:30
            0.06,  // 13:30-14:00
            0.08,  // 14:00-14:30
            0.10,  // 14:30-15:00
            0.09,  // 15:00-15:30
            0.10   // 15:30-16:00 (장 마감 직전 높음)
    };

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.VWAP;
    }

    @Override
    public List<SlicePlan> planSlices(AlgorithmicOrder order) {
        AlgorithmicOrder.AlgorithmParameters params = order.getParameters();
        if (params == null) {
            params = AlgorithmicOrder.AlgorithmParameters.defaultVwap();
        }

        int totalQuantity = order.getTotalQuantity();
        int slices = Math.max(1, params.getSlices());
        int intervalMinutes = params.getIntervalMinutes();
        int minSliceQuantity = params.getMinSliceQuantity();

        double[] volumeProfile = "US".equalsIgnoreCase(order.getMarket())
                ? US_VOLUME_PROFILE
                : KR_VOLUME_PROFILE;

        double[] adjustedProfile = adjustVolumeProfile(volumeProfile, slices);

        List<SlicePlan> plans = new ArrayList<>();
        int allocatedQuantity = 0;

        for (int i = 0; i < slices; i++) {
            int sliceQty;
            if (i == slices - 1) {
                sliceQty = totalQuantity - allocatedQuantity;
            } else {
                sliceQty = (int) Math.round(totalQuantity * adjustedProfile[i]);
                sliceQty = Math.max(sliceQty, minSliceQuantity);
            }

            allocatedQuantity += sliceQty;
            long delayMillis = (long) i * intervalMinutes * 60 * 1000;

            plans.add(new SlicePlan(i + 1, sliceQty, delayMillis, adjustedProfile[i]));
        }

        if (allocatedQuantity != totalQuantity) {
            SlicePlan last = plans.get(plans.size() - 1);
            int adjustment = totalQuantity - allocatedQuantity;
            plans.set(plans.size() - 1, new SlicePlan(
                    last.sliceNumber(),
                    last.quantity() + adjustment,
                    last.delayMillis(),
                    last.targetVolumeRatio()
            ));
        }

        return plans;
    }

    private double[] adjustVolumeProfile(double[] baseProfile, int slices) {
        double[] adjusted = new double[slices];
        double total = 0;

        for (int i = 0; i < slices; i++) {
            int profileIndex = (int) ((double) i / slices * baseProfile.length);
            profileIndex = Math.min(profileIndex, baseProfile.length - 1);
            adjusted[i] = baseProfile[profileIndex];
            total += adjusted[i];
        }

        for (int i = 0; i < slices; i++) {
            adjusted[i] = adjusted[i] / total;
        }

        return adjusted;
    }

    /**
     * VWAP 계산.
     *
     * @param prices 체결 가격 목록
     * @param quantities 체결 수량 목록
     * @return VWAP
     */
    public static BigDecimal calculateVwap(List<BigDecimal> prices, List<Integer> quantities) {
        if (prices.isEmpty() || prices.size() != quantities.size()) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumPriceVolume = BigDecimal.ZERO;
        int sumVolume = 0;

        for (int i = 0; i < prices.size(); i++) {
            BigDecimal price = prices.get(i);
            int quantity = quantities.get(i);

            sumPriceVolume = sumPriceVolume.add(price.multiply(BigDecimal.valueOf(quantity)));
            sumVolume += quantity;
        }

        if (sumVolume == 0) {
            return BigDecimal.ZERO;
        }

        return sumPriceVolume.divide(BigDecimal.valueOf(sumVolume), 4, RoundingMode.HALF_UP);
    }

    /**
     * 실제 VWAP와 시장 VWAP 괴리율 계산 (%).
     *
     * @param actualVwap 실제 체결 VWAP
     * @param marketVwap 시장 VWAP
     * @return 괴리율 (%)
     */
    public static BigDecimal calculateSlippage(BigDecimal actualVwap, BigDecimal marketVwap) {
        if (marketVwap == null || marketVwap.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return actualVwap.subtract(marketVwap)
                .divide(marketVwap, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
