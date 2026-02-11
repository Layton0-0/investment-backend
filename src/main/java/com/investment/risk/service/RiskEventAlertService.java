package com.investment.risk.service;

import com.investment.alert.EmergencyAlertService;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.RiskProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.RiskAccountSummaryDto;
import com.investment.risk.dto.RiskSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 리스크 이벤트 알림 — 일일 손실 한도 임박·VaR 95% 초과 시 Discord 발송 및 TB_ALERT_LOG 저장.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskEventAlertService {

    private static final String COMPONENT_DAILY_LOSS_APPROACHING = "DailyLossApproaching";
    private static final String COMPONENT_VAR_EXCEEDED = "VarExceeded";
    private static final Duration THROTTLE_MIN_INTERVAL = Duration.ofMinutes(60);

    private final EmergencyAlertService emergencyAlertService;
    private final RiskReportService riskReportService;
    private final RiskProperties riskProperties;
    private final TradingSettingRepository tradingSettingRepository;

    private final Map<String, Instant> lastSentByKey = new ConcurrentHashMap<>();

    /**
     * 자동투자 ON 계좌에 대해 일일 손실 한도 임박·VaR 초과 여부를 검사하고, 조건 충족 시 알림 발송.
     * Discord Webhook 미설정 시 스킵. 동일 계좌·동일 이벤트는 60분 간격으로만 발송(스팸 방지).
     */
    @Transactional(readOnly = true)
    public void checkAndSendAlerts() {
        BigDecimal alertMdd = riskProperties.getAlertMddThresholdPct();
        boolean mddAlertEnabled = alertMdd != null && alertMdd.compareTo(BigDecimal.ZERO) > 0;
        if (!mddAlertEnabled && !riskProperties.isAlertVarExceedEnabled()) {
            return;
        }
        List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
        if (settings == null || settings.isEmpty()) {
            return;
        }
        List<String> userIds = settings.stream()
                .map(TradingSetting::getUserId)
                .distinct()
                .toList();
        for (String userId : userIds) {
            try {
                RiskSummaryDto summary = riskReportService.getSummary(userId);
                if (summary == null || summary.getAccounts() == null) {
                    continue;
                }
                BigDecimal dailyLimitPct = riskProperties.getDailyLossLimitPct();
                BigDecimal alertMddThreshold = riskProperties.getAlertMddThresholdPct();
                BigDecimal var95Pct = summary.getVar95Pct();
                boolean varExceedEnabled = riskProperties.isAlertVarExceedEnabled();

                for (RiskAccountSummaryDto account : summary.getAccounts()) {
                    BigDecimal opening = account.getOpeningBalance();
                    BigDecimal current = account.getCurrentValue();
                    if (opening == null || opening.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    if (current == null) {
                        current = BigDecimal.ZERO;
                    }
                    // 당일 손실률 (%) = (시초 - 현재) / 시초 * 100
                    BigDecimal lossPct = opening.subtract(current)
                            .divide(opening, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    if (lossPct.compareTo(BigDecimal.ZERO) < 0) {
                        continue;
                    }
                    String accountMasked = account.getAccountNoMasked() != null
                            ? account.getAccountNoMasked() : "****";
                    String serverLabel = "1".equals(account.getServerType()) ? "모의" : "실전";

                    // 일일 손실 한도 임박 (한도 대비 alertMddThreshold 비율 도달)
                    if (alertMddThreshold != null && dailyLimitPct != null
                            && dailyLimitPct.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal ratio = lossPct.divide(dailyLimitPct, 4, RoundingMode.HALF_UP);
                        if (ratio.compareTo(alertMddThreshold) >= 0) {
                            String key = throttleKey(userId, accountMasked, COMPONENT_DAILY_LOSS_APPROACHING);
                            if (shouldSend(key)) {
                                String message = String.format(
                                        "** [리스크] 일일 손실 한도 임박 **\n계좌: %s\n모의/실전: %s\n당일 손실률: %s%%\n한도: %s%% (%.0f%% 도달)\n",
                                        accountMasked, serverLabel, lossPct.setScale(2, RoundingMode.HALF_UP),
                                        dailyLimitPct, ratio.multiply(BigDecimal.valueOf(100)).doubleValue());
                                emergencyAlertService.sendRiskEventAlert("WARNING", COMPONENT_DAILY_LOSS_APPROACHING, message);
                                lastSentByKey.put(key, Instant.now());
                                log.warn("리스크 이벤트 알림 발송: 일일 손실 한도 임박, account={}", accountMasked);
                            }
                        }
                    }

                    // VaR 95% 초과 (당일 손실률이 VaR 95% 한도를 초과)
                    if (varExceedEnabled && var95Pct != null && var95Pct.compareTo(BigDecimal.ZERO) > 0
                            && lossPct.compareTo(var95Pct) > 0) {
                        String key = throttleKey(userId, accountMasked, COMPONENT_VAR_EXCEEDED);
                        if (shouldSend(key)) {
                            String message = String.format(
                                    "** [리스크] VaR 95%% 초과 **\n계좌: %s\n모의/실전: %s\n당일 손실률: %s%%\nVaR 95%%: %s%%\n",
                                    accountMasked, serverLabel, lossPct.setScale(2, RoundingMode.HALF_UP), var95Pct);
                            emergencyAlertService.sendRiskEventAlert("ERROR", COMPONENT_VAR_EXCEEDED, message);
                            lastSentByKey.put(key, Instant.now());
                            log.warn("리스크 이벤트 알림 발송: VaR 95% 초과, account={}", accountMasked);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("리스크 이벤트 검사 실패: userId={}, error={}", LogMaskingUtil.maskUserId(userId), e.getMessage());
            }
        }
    }

    private static String throttleKey(String userId, String accountMasked, String component) {
        return (userId != null ? userId : "") + "|" + (accountMasked != null ? accountMasked : "") + "|" + component;
    }

    private boolean shouldSend(String key) {
        Instant last = lastSentByKey.get(key);
        if (last == null) {
            return true;
        }
        return Duration.between(last, Instant.now()).compareTo(THROTTLE_MIN_INTERVAL) >= 0;
    }
}
