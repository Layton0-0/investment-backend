package com.investment.factor.scheduler;

import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.factor.service.DailyPnlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 일일 PnL 집계 스케줄러 (P3).
 * 장 마감 후 계좌별 당일 수익률 기록.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyPnlScheduler {

    private final TradingSettingRepository tradingSettingRepository;
    private final DailyPnlService dailyPnlService;

    /** Spring Batch Job에서 호출. */
    public void recordDailyPnl() {
        List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
        if (settings.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (TradingSetting setting : settings) {
            try {
                dailyPnlService.recordDailyPnl(setting.getAccountNo(), today);
            } catch (Exception e) {
                log.warn("일일 PnL 기록 실패: accountNo={}, error={}", setting.getAccountNo(), e.getMessage());
            }
        }
    }
}
