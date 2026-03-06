package com.investment.marketdata.scheduler;

import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.websocket.KoreaInvestmentWebSocketClient;
import com.investment.setting.service.SystemSettingService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DB 설정(marketData.websocketEnabled=true)일 때 장 시작 전(기본 08:50 KST) WebSocket 연결, 장 종료 후(15:35 KST) 연결 해제를 스케줄합니다.
 * UserApiKeyRepository에서 KOREA_INVESTMENT 건마다 KoreaInvestmentWebSocketClient.connect/disconnect를 호출합니다.
 *
 * @see KoreaInvestmentWebSocketClient
 * @see MarketDataProperties.KoreaInvestmentProperties.WebSocketProperties
 * @see SystemSettingService#getBoolean(String) marketData.websocketEnabled
 */
@Slf4j
@Component
public class WebSocketConnectScheduler {

    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private static final long INITIAL_CONNECT_DELAY_SECONDS = 5L;
    private static final String WEBSOCKET_ENABLED_KEY = "marketData.websocketEnabled";

    private final TaskScheduler taskScheduler;
    private final UserApiKeyRepository userApiKeyRepository;
    private final MarketDataProperties marketDataProperties;
    private final ObjectProvider<KoreaInvestmentWebSocketClient> webSocketClientProvider;
    private final SystemSettingService systemSettingService;

    public WebSocketConnectScheduler(TaskScheduler taskScheduler,
                                     UserApiKeyRepository userApiKeyRepository,
                                     MarketDataProperties marketDataProperties,
                                     ObjectProvider<KoreaInvestmentWebSocketClient> webSocketClientProvider,
                                     SystemSettingService systemSettingService) {
        this.taskScheduler = taskScheduler;
        this.userApiKeyRepository = userApiKeyRepository;
        this.marketDataProperties = marketDataProperties;
        this.webSocketClientProvider = webSocketClientProvider;
        this.systemSettingService = systemSettingService;
    }

    @PostConstruct
    public void schedule() {
        try {
            if (!Boolean.TRUE.equals(systemSettingService.getBoolean(WEBSOCKET_ENABLED_KEY))) {
                log.debug("WebSocket 비활성(marketData.websocketEnabled=false), 스케줄 미등록");
                return;
            }
        } catch (Exception e) {
            log.warn("WebSocket 설정 조회 실패, 스케줄 미등록: {}", e.getMessage());
            return;
        }
        var ws = marketDataProperties.getKoreaInvestment().getWebsocket();
        String connectCron = ws.getConnectCron();
        String disconnectCron = ws.getDisconnectCron();

        if (connectCron != null && !connectCron.isBlank()) {
            try {
                taskScheduler.schedule(this::runConnect, new CronTrigger(connectCron, ZONE_KST));
                log.info("Scheduled WebSocket connect: cron={}, zone=Asia/Seoul", connectCron);
                taskScheduler.schedule(this::runConnect, Instant.now().plusSeconds(INITIAL_CONNECT_DELAY_SECONDS));
                log.info("Scheduled one-time WebSocket connect in {} seconds", INITIAL_CONNECT_DELAY_SECONDS);
            } catch (Exception e) {
                log.warn("Failed to schedule WebSocket connect: cron={}", connectCron, e);
            }
        }

        if (disconnectCron != null && !disconnectCron.isBlank()) {
            try {
                taskScheduler.schedule(this::runDisconnect, new CronTrigger(disconnectCron, ZONE_KST));
                log.info("Scheduled WebSocket disconnect: cron={}, zone=Asia/Seoul", disconnectCron);
            } catch (Exception e) {
                log.warn("Failed to schedule WebSocket disconnect: cron={}", disconnectCron, e);
            }
        }
    }

    /**
     * KOREA_INVESTMENT 사용자·serverType별로 WebSocket 연결.
     */
    public void runConnect() {
        KoreaInvestmentWebSocketClient client = webSocketClientProvider.getIfAvailable();
        if (client == null) {
            log.debug("KoreaInvestmentWebSocketClient not available, skip WebSocket connect");
            return;
        }
        List<UserApiKey> keys = koreaInvestmentKeys();
        if (keys.isEmpty()) {
            log.debug("No KOREA_INVESTMENT API keys, skip WebSocket connect");
            return;
        }
        for (UserApiKey key : keys) {
            try {
                client.connect(key.getUserId(), key.getServerType());
            } catch (Exception e) {
                log.error("WebSocket connect failed: userId={}, serverType={}", key.getUserId(), key.getServerType(), e);
            }
        }
        log.info("WebSocket connect run finished: {} sessions", keys.size());
    }

    /**
     * KOREA_INVESTMENT 사용자·serverType별로 WebSocket 연결 해제.
     */
    public void runDisconnect() {
        KoreaInvestmentWebSocketClient client = webSocketClientProvider.getIfAvailable();
        if (client == null) {
            log.debug("KoreaInvestmentWebSocketClient not available, skip WebSocket disconnect");
            return;
        }
        List<UserApiKey> keys = koreaInvestmentKeys();
        if (keys.isEmpty()) {
            log.debug("No KOREA_INVESTMENT API keys, skip WebSocket disconnect");
            return;
        }
        for (UserApiKey key : keys) {
            try {
                client.disconnect(key.getUserId(), key.getServerType());
            } catch (Exception e) {
                log.error("WebSocket disconnect failed: userId={}, serverType={}", key.getUserId(), key.getServerType(), e);
            }
        }
        log.info("WebSocket disconnect run finished: {} sessions", keys.size());
    }

    private List<UserApiKey> koreaInvestmentKeys() {
        return userApiKeyRepository.findAll().stream()
                .filter(k -> k.getBrokerType() == BrokerType.KOREA_INVESTMENT)
                .collect(Collectors.toList());
    }
}
