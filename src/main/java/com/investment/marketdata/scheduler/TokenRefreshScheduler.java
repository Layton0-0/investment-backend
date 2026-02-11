package com.investment.marketdata.scheduler;

import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;

/**
 * 장 시작 30분 전(기본 08:30 KST) 한국투자증권 Access Token을 전 사용자·모의/실 serverType별로 강제 갱신합니다.
 * 24시간 유효 토큰을 장 전에 새로 발급해 당일 거래 안정성을 확보합니다.
 *
 * @see KoreaInvestmentTokenService#forceRefreshAllTokensForMarketOpen()
 * @see MarketDataProperties.KoreaInvestmentProperties.TokenRefreshProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "investment.market-data.korea-investment.token",
        name = "pre-market-refresh-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class TokenRefreshScheduler {

    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    private final TaskScheduler taskScheduler;
    private final KoreaInvestmentTokenService tokenService;
    private final MarketDataProperties marketDataProperties;

    @PostConstruct
    public void scheduleTokenRefresh() {
        String cron = marketDataProperties.getKoreaInvestment().getToken().getPreMarketRefreshCron();
        if (cron == null || cron.isBlank()) {
            log.debug("Token refresh cron not set, skipping schedule");
            return;
        }
        try {
            CronTrigger trigger = new CronTrigger(cron, ZONE_KST);
            taskScheduler.schedule(this::runTokenRefresh, trigger);
            log.info("Scheduled Korea Investment token refresh: cron={}, zone=Asia/Seoul", cron);
        } catch (Exception e) {
            log.warn("Failed to schedule token refresh: cron={}", cron, e);
        }
    }

    private void runTokenRefresh() {
        try {
            tokenService.forceRefreshAllTokensForMarketOpen();
        } catch (Exception e) {
            log.error("Token refresh execution failed", e);
        }
    }
}
