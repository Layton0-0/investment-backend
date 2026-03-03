package com.investment.marketdata.scheduler;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.websocket.KoreaInvestmentWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 활성화 시 한국투자증권 실시간 연결·구독을 수행합니다.
 * 기동 후 일정 시간 뒤 1회 연결하고, 설정된 경우 장 시작 전 크론으로 재연결·구독을 실행합니다.
 *
 * @see KoreaInvestmentWebSocketClient#connect(String, String)
 * @see KoreaInvestmentWebSocketClient#subscribeCcnlNotice(String, String)
 * @see MarketDataProperties.KoreaInvestmentProperties.WebSocketProperties
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "investment.market-data.korea-investment.websocket.enabled",
        havingValue = "true")
public class WebSocketConnectScheduler {

    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    /** 기동 후 연결 지연(초). 토큰 갱신 등 선행 작업 여유 */
    private static final int CONNECT_DELAY_SECONDS = 5;

    private final UserApiKeyRepository userApiKeyRepository;
    private final KoreaInvestmentWebSocketClient webSocketClient;
    private final MarketDataProperties marketDataProperties;
    private final TaskScheduler taskScheduler;

    @PostConstruct
    public void init() {
        taskScheduler.schedule(
                this::runConnectForAllUsers,
                java.time.Instant.now().plusSeconds(CONNECT_DELAY_SECONDS));
        log.info("WebSocket 연결 스케줄: 기동 {}초 후 1회 실행 예약", CONNECT_DELAY_SECONDS);

        String connectCron = marketDataProperties.getKoreaInvestment().getWebsocket().getConnectCron();
        if (connectCron != null && !connectCron.isBlank()) {
            try {
                CronTrigger trigger = new CronTrigger(connectCron.trim(), ZONE_KST);
                taskScheduler.schedule(this::runConnectForAllUsers, trigger);
                log.info("WebSocket 장 시작 전 연결 스케줄: cron={}, zone=Asia/Seoul", connectCron.trim());
            } catch (Exception e) {
                log.warn("WebSocket connect cron 등록 실패: cron={}", connectCron, e);
            }
        }
    }

    /**
     * 한국투자증권 API 키가 있는 모든 (userId, serverType)에 대해 WebSocket 연결 및 체결통보 구독.
     */
    public void runConnectForAllUsers() {
        List<UserApiKey> all = userApiKeyRepository.findAll();
        int connected = 0;
        for (UserApiKey key : all) {
            if (key.getBrokerType() != BrokerType.KOREA_INVESTMENT) {
                continue;
            }
            String userId = key.getUserId();
            String serverType = key.getServerType() != null ? key.getServerType() : "1";
            try {
                webSocketClient.connect(userId, serverType);
                webSocketClient.subscribeCcnlNotice(userId, serverType);
                connected++;
                log.debug("WebSocket 연결·체결통보 구독: userId={}, serverType={}",
                        LogMaskingUtil.maskUserId(userId), serverType);
            } catch (Exception e) {
                log.warn("WebSocket 연결 실패: userId={}, serverType={}, error={}",
                        LogMaskingUtil.maskUserId(userId), serverType, e.getMessage());
            }
        }
        if (connected > 0) {
            log.info("WebSocket 연결·구독 완료: {}건", connected);
        }
    }
}
