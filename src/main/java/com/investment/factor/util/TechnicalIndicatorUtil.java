package com.investment.factor.util;

import com.investment.domain.entity.DailyStock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DailyStock 일봉 기반 기술적 지표 계산 (RSI, MACD).
 * 단기 시그널 필터(RSI&gt;60 &amp; MACD&gt;Signal) 등에서 사용.
 */
public final class TechnicalIndicatorUtil {

    private static final int DEFAULT_RSI_PERIOD = 14;
    private static final int MACD_FAST = 12;
    private static final int MACD_SLOW = 26;
    private static final int MACD_SIGNAL_PERIOD = 9;

    private TechnicalIndicatorUtil() {
    }

    /**
     * RSI(14) 계산. 기준일(basDt) 기준 최신 종가 포함 일봉 리스트 사용.
     *
     * @param history basDt 기준 과거 일봉 (basDt 오름차순 정렬 권장). 최소 period+1개 필요.
     * @return RSI 값 (0~100). 데이터 부족 시 empty.
     */
    public static Optional<BigDecimal> computeRsi(List<DailyStock> history, int period) {
        if (history == null || history.size() < period + 1) {
            return Optional.empty();
        }
        List<DailyStock> sorted = history.stream()
                .sorted(Comparator.comparing(DailyStock::getBasDt))
                .collect(Collectors.toList());
        double[] closes = sorted.stream()
                .map(DailyStock::getClosePrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
        if (closes.length < period + 1) {
            return Optional.empty();
        }
        double rsi = calculateRsi(closes, period);
        return Optional.of(BigDecimal.valueOf(rsi).setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * RSI(14) 계산 (기본 기간).
     */
    public static Optional<BigDecimal> computeRsi(List<DailyStock> history) {
        return computeRsi(history, DEFAULT_RSI_PERIOD);
    }

    /**
     * MACD(12, 26, 9) 계산. Signal = MACD 라인의 9일 EMA.
     *
     * @param history basDt 기준 과거 일봉 (basDt 오름차순). 최소 26+9=35개 권장.
     * @return MACD 값, Signal 값. 데이터 부족 시 empty.
     */
    public static Optional<MacdResult> computeMacd(List<DailyStock> history) {
        if (history == null || history.size() < MACD_SLOW + MACD_SIGNAL_PERIOD) {
            return Optional.empty();
        }
        List<DailyStock> sorted = history.stream()
                .sorted(Comparator.comparing(DailyStock::getBasDt))
                .collect(Collectors.toList());
        double[] closes = sorted.stream()
                .map(DailyStock::getClosePrice)
                .filter(p -> p != null && p.compareTo(BigDecimal.ZERO) > 0)
                .mapToDouble(BigDecimal::doubleValue)
                .toArray();
        if (closes.length < MACD_SLOW + MACD_SIGNAL_PERIOD) {
            return Optional.empty();
        }
        double[] macdLine = new double[closes.length];
        double ema12 = closes[0];
        double ema26 = closes[0];
        double multiplier12 = 2.0 / (MACD_FAST + 1);
        double multiplier26 = 2.0 / (MACD_SLOW + 1);
        for (int i = 1; i < closes.length; i++) {
            ema12 = (closes[i] * multiplier12) + (ema12 * (1 - multiplier12));
            ema26 = (closes[i] * multiplier26) + (ema26 * (1 - multiplier26));
            macdLine[i] = ema12 - ema26;
        }
        // Signal = 9일 EMA of MACD line (첫 9개는 누적 후 평균으로 초기값)
        int start = MACD_SLOW;
        double signal = macdLine[start];
        double multSig = 2.0 / (MACD_SIGNAL_PERIOD + 1);
        for (int i = start + 1; i < macdLine.length; i++) {
            signal = (macdLine[i] * multSig) + (signal * (1 - multSig));
        }
        double macd = macdLine[macdLine.length - 1];
        double hist = macd - signal;
        return Optional.of(new MacdResult(
                BigDecimal.valueOf(macd).setScale(4, RoundingMode.HALF_UP),
                BigDecimal.valueOf(signal).setScale(4, RoundingMode.HALF_UP),
                BigDecimal.valueOf(hist).setScale(4, RoundingMode.HALF_UP)));
    }

    /**
     * MACD &gt; Signal 여부. 단기 필터용.
     */
    public static boolean isMacdAboveSignal(List<DailyStock> history) {
        return computeMacd(history)
                .map(m -> m.getMacd().compareTo(m.getSignal()) > 0)
                .orElse(false);
    }

    /**
     * RSI &gt; threshold (예: 60) 여부.
     */
    public static boolean isRsiAbove(List<DailyStock> history, int period, BigDecimal threshold) {
        return computeRsi(history, period)
                .filter(rsi -> rsi.compareTo(threshold) > 0)
                .isPresent();
    }

    private static double calculateRsi(double[] closes, int period) {
        if (closes.length < period + 1) {
            return 50.0;
        }
        double[] gains = new double[closes.length - 1];
        double[] losses = new double[closes.length - 1];
        for (int i = 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            gains[i - 1] = change > 0 ? change : 0;
            losses[i - 1] = change < 0 ? -change : 0;
        }
        int last = gains.length - 1;
        double avgGain = 0;
        double avgLoss = 0;
        for (int i = last - period + 1; i <= last; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        avgGain /= period;
        avgLoss /= period;
        if (avgLoss == 0) {
            return 100.0;
        }
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class MacdResult {
        private final BigDecimal macd;
        private final BigDecimal signal;
        private final BigDecimal histogram;
    }
}
