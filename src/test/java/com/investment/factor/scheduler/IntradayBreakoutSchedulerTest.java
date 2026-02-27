package com.investment.factor.scheduler;

import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.IntradayBreakoutService;
import com.investment.factor.service.RiskGateService;
import com.investment.order.service.OrderService;
import com.investment.setting.service.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IntradayBreakoutScheduler")
class IntradayBreakoutSchedulerTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;
    @Mock
    private IntradayBreakoutService intradayBreakoutService;
    @Mock
    private StrategyPositionRepository strategyPositionRepository;
    @Mock
    private RiskGateService riskGateService;
    @Mock
    private DailyLossLimitService dailyLossLimitService;
    @Mock
    private OrderService orderService;
    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private IntradayBreakoutScheduler intradayBreakoutScheduler;

    @Test
    @DisplayName("breakout 비활성 시 getBreakoutCandidates 미호출")
    void runIntradayBreakout_disabled_skips() {
        ReflectionTestUtils.setField(intradayBreakoutScheduler, "breakoutEnabled", false);

        intradayBreakoutScheduler.runIntradayBreakout();

        verify(intradayBreakoutService, never()).getBreakoutCandidates(any(), any(), any());
    }

    @Test
    @DisplayName("자동투자 ON 계좌 없으면 스킵")
    void runIntradayBreakout_noSettings_skips() {
        ReflectionTestUtils.setField(intradayBreakoutScheduler, "breakoutEnabled", true);
        when(systemSettingService.getBoolean("pipeline.autoExecute")).thenReturn(true);
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of());

        intradayBreakoutScheduler.runIntradayBreakout();

        verify(intradayBreakoutService, never()).getBreakoutCandidates(any(), any(), any());
    }
}
