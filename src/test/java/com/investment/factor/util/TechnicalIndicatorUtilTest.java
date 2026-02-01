package com.investment.factor.util;

import com.investment.domain.entity.DailyStock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TechnicalIndicatorUtil")
class TechnicalIndicatorUtilTest {

    @Test
    @DisplayName("computeRsi 데이터 부족 시 empty")
    void computeRsi_insufficientData_returnsEmpty() {
        List<DailyStock> few = List.of(
                dailyStock(LocalDate.now().minusDays(1), "100"),
                dailyStock(LocalDate.now(), "102"));
        assertThat(TechnicalIndicatorUtil.computeRsi(few, 14)).isEmpty();
        assertThat(TechnicalIndicatorUtil.computeRsi(List.of())).isEmpty();
        assertThat(TechnicalIndicatorUtil.computeRsi(null)).isEmpty();
    }

    @Test
    @DisplayName("computeRsi 충분한 일봉 시 RSI 0~100 범위")
    void computeRsi_sufficientData_returnsInRange() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 20; i >= 0; i--) {
            history.add(dailyStock(LocalDate.now().minusDays(i), String.valueOf(100 + i)));
        }
        Optional<BigDecimal> rsi = TechnicalIndicatorUtil.computeRsi(history, 14);
        assertThat(rsi).isPresent();
        assertThat(rsi.get().doubleValue()).isBetween(0.0, 100.0);
    }

    @Test
    @DisplayName("isRsiAbove threshold에 따라 true/false 반환")
    void isRsiAbove_returnsBooleanByThreshold() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 30; i >= 0; i--) {
            history.add(dailyStock(LocalDate.now().minusDays(i), String.valueOf(95 + i)));
        }
        Optional<BigDecimal> rsiOpt = TechnicalIndicatorUtil.computeRsi(history, 14);
        assertThat(rsiOpt).isPresent();
        BigDecimal rsi = rsiOpt.get();
        assertThat(TechnicalIndicatorUtil.isRsiAbove(history, 14, rsi.subtract(BigDecimal.ONE))).isTrue();
        assertThat(TechnicalIndicatorUtil.isRsiAbove(history, 14, rsi.add(BigDecimal.ONE))).isFalse();
    }

    @Test
    @DisplayName("computeMacd 데이터 부족 시 empty")
    void computeMacd_insufficientData_returnsEmpty() {
        List<DailyStock> few = new ArrayList<>();
        for (int i = 20; i >= 0; i--) {
            few.add(dailyStock(LocalDate.now().minusDays(i), String.valueOf(100)));
        }
        assertThat(TechnicalIndicatorUtil.computeMacd(few)).isEmpty();
        assertThat(TechnicalIndicatorUtil.computeMacd(List.of())).isEmpty();
        assertThat(TechnicalIndicatorUtil.computeMacd(null)).isEmpty();
    }

    @Test
    @DisplayName("computeMacd 충분한 일봉 시 MacdResult 반환")
    void computeMacd_sufficientData_returnsResult() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 40; i >= 0; i--) {
            history.add(dailyStock(LocalDate.now().minusDays(i), String.valueOf(100 + (i % 5))));
        }
        Optional<TechnicalIndicatorUtil.MacdResult> macd = TechnicalIndicatorUtil.computeMacd(history);
        assertThat(macd).isPresent();
        assertThat(macd.get().getMacd()).isNotNull();
        assertThat(macd.get().getSignal()).isNotNull();
        assertThat(macd.get().getHistogram()).isNotNull();
    }

    @Test
    @DisplayName("isMacdAboveSignal 데이터 있으면 boolean 반환")
    void isMacdAboveSignal_returnsBoolean() {
        List<DailyStock> history = new ArrayList<>();
        for (int i = 40; i >= 0; i--) {
            history.add(dailyStock(LocalDate.now().minusDays(i), String.valueOf(100 + i)));
        }
        boolean result = TechnicalIndicatorUtil.isMacdAboveSignal(history);
        assertThat(result).isEqualTo(TechnicalIndicatorUtil.computeMacd(history)
                .map(m -> m.getMacd().compareTo(m.getSignal()) > 0)
                .orElse(false));
    }

    private static DailyStock dailyStock(LocalDate basDt, String close) {
        return DailyStock.builder()
                .basDt(basDt)
                .symbol("005930")
                .market("KR")
                .closePrice(new BigDecimal(close))
                .build();
    }
}
