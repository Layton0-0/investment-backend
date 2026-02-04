package com.investment.factor.scheduler;

import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.service.DailyPnlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyPnlScheduler")
class DailyPnlSchedulerTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;
    @Mock
    private DailyPnlService dailyPnlService;

    @InjectMocks
    private DailyPnlScheduler dailyPnlScheduler;

    @Test
    @DisplayName("자동투자 ON 계좌 없으면 recordDailyPnl 미호출")
    void recordDailyPnl_noSettings_skips() {
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of());

        dailyPnlScheduler.recordDailyPnl();

        verify(dailyPnlService, never()).recordDailyPnl(any(), any());
    }

    @Test
    @DisplayName("자동투자 ON 계좌 있으면 계좌별 recordDailyPnl 호출")
    void recordDailyPnl_withSettings_callsService() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("1234567890")
                .maxInvestmentAmount(java.math.BigDecimal.valueOf(10_000_000))
                .minInvestmentAmount(java.math.BigDecimal.valueOf(10_000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .build();
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of(setting));

        dailyPnlScheduler.recordDailyPnl();

        verify(dailyPnlService, times(1)).recordDailyPnl(eq("1234567890"), any());
    }
}
