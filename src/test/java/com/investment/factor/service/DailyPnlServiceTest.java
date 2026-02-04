package com.investment.factor.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPnlService")
class DailyPnlServiceTest {

    @Mock
    private DailyLossLimitService dailyLossLimitService;

    @InjectMocks
    private DailyPnlService dailyPnlService;

    private static final String ACCOUNT_NO = "1234567890";
    private static final LocalDate DATE = LocalDate.of(2026, 2, 3);

    @Test
    @DisplayName("accountNo null 시 null 반환")
    void recordDailyPnl_accountNoNull_returnsNull() {
        BigDecimal result = dailyPnlService.recordDailyPnl(null, DATE);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("date null 시 null 반환")
    void recordDailyPnl_dateNull_returnsNull() {
        BigDecimal result = dailyPnlService.recordDailyPnl(ACCOUNT_NO, null);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("시초 평가액 없음 시 null 반환")
    void recordDailyPnl_noOpening_returnsNull() {
        when(dailyLossLimitService.getOpeningBalance(ACCOUNT_NO, DATE)).thenReturn(null);
        when(dailyLossLimitService.getCurrentPortfolioValue(ACCOUNT_NO)).thenReturn(new BigDecimal("10000000"));

        BigDecimal result = dailyPnlService.recordDailyPnl(ACCOUNT_NO, DATE);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("현재 평가액 조회 실패 시 null 반환")
    void recordDailyPnl_noClosing_returnsNull() {
        when(dailyLossLimitService.getOpeningBalance(ACCOUNT_NO, DATE)).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.getCurrentPortfolioValue(ACCOUNT_NO)).thenReturn(null);

        BigDecimal result = dailyPnlService.recordDailyPnl(ACCOUNT_NO, DATE);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("시초·종료 있으면 일일 수익률(%) 계산 후 반환")
    void recordDailyPnl_success_returnsPnlPct() {
        when(dailyLossLimitService.getOpeningBalance(ACCOUNT_NO, DATE)).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.getCurrentPortfolioValue(ACCOUNT_NO)).thenReturn(new BigDecimal("10300000"));

        BigDecimal result = dailyPnlService.recordDailyPnl(ACCOUNT_NO, DATE);

        assertThat(result).isEqualByComparingTo(new BigDecimal("3"));
    }

    @Test
    @DisplayName("손실일 때 음수 수익률 반환")
    void recordDailyPnl_loss_returnsNegativePct() {
        when(dailyLossLimitService.getOpeningBalance(ACCOUNT_NO, DATE)).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.getCurrentPortfolioValue(ACCOUNT_NO)).thenReturn(new BigDecimal("9700000"));

        BigDecimal result = dailyPnlService.recordDailyPnl(ACCOUNT_NO, DATE);

        assertThat(result).isEqualByComparingTo(new BigDecimal("-3"));
    }
}
