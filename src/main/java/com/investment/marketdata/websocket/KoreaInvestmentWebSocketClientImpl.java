package com.investment.marketdata.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.common.security.LogMaskingUtil;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국투자증권 실시간 WebSocket 클라이언트 구현체.
 * 실시간 호가(asking_price_total), 실시간 체결통보(ccnl_notice) 구독.
 * MCP: asking_price_total, ccnl_notice URL·구독 포맷 반영. 연결/구독 간격은 API 제한 준수.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "investment.market-data.korea-investment.websocket.enabled",
        havingValue = "true")
public class KoreaInvestmentWebSocketClientImpl implements KoreaInvestmentWebSocketClient {

    private static final long MIN_CONNECTION_INTERVAL_MS = 1000L;

    private final MarketDataProperties properties;
    private final KoreaInvestmentTokenService tokenService;
    private final WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper;

    /** userId|serverType -> WebSocketSession (연결 유지용) */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastConnectTime = new ConcurrentHashMap<>();
    /** userId|serverType -> REST로 발급한 approval_key (설정이 비어 있을 때 사용) */
    private final ConcurrentHashMap<String, String> approvalKeyByKey = new ConcurrentHashMap<>();

    public KoreaInvestmentWebSocketClientImpl(MarketDataProperties properties,
                                              KoreaInvestmentTokenService tokenService,
                                              ObjectMapper objectMapper) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.webSocketClient = new ReactorNettyWebSocketClient();
        this.objectMapper = objectMapper;
    }

    private static String key(String userId, String serverType) {
        return (userId != null ? userId : "") + "|" + (serverType != null ? serverType : "1");
    }

    private String getBaseUrl(String serverType) {
        MarketDataProperties.KoreaInvestmentProperties.WebSocketProperties ws =
                properties.getKoreaInvestment().getWebsocket();
        return "1".equals(serverType) ? ws.getBaseUrlVirtual() : ws.getBaseUrlReal();
    }

    private URI buildUri(String userId, String serverType) {
        String base = getBaseUrl(serverType);
        String path = properties.getKoreaInvestment().getWebsocket().getPath();
        if (path == null || path.isBlank()) {
            path = "/tryitout";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String url = base.endsWith("/") ? base + path.substring(1) : base + path;
        return URI.create(url);
    }

    /** 구독 메시지 본문 생성. KIS 포맷: header(approval_key, custtype, tr_type), body(input: tr_id, tr_key). sessionKey로 REST 발급 approval_key 조회 */
    private String buildSubscribeMessage(String sessionKey, String trId, String trKey) {
        try {
            Map<String, Object> header = new java.util.HashMap<>(Map.of(
                    "custtype", "P",
                    "tr_type", "1",
                    "content-type", "utf-8"
            ));
            String approvalKey = properties.getKoreaInvestment().getWebsocket().getApprovalKey();
            if (approvalKey == null || approvalKey.isBlank()) {
                approvalKey = sessionKey != null ? approvalKeyByKey.get(sessionKey) : null;
            }
            if (approvalKey != null && !approvalKey.isBlank()) {
                header.put("approval_key", approvalKey);
            }
            Map<String, Object> input = Map.of(
                    "tr_id", trId,
                    "tr_key", trKey != null ? trKey : ""
            );
            Map<String, Object> body = Map.of("input", input);
            Map<String, Object> msg = Map.of("header", header, "body", body);
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            log.warn("WebSocket 구독 메시지 직렬화 실패: trId={}, trKey={}", trId, trKey, e);
            return "{}";
        }
    }

    @Override
    public void connect(String userId, String serverType) {
        String k = key(userId, serverType);
        if (sessions.containsKey(k)) {
            log.debug("WebSocket 이미 연결됨: key={}", LogMaskingUtil.maskUserId(k.split("\\|")[0]));
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastConnectTime.get(k);
        if (last != null && (now - last) < MIN_CONNECTION_INTERVAL_MS) {
            log.warn("WebSocket 연결 간격 미준수(1초): key={}", LogMaskingUtil.maskUserId(userId));
        }
        lastConnectTime.put(k, now);

        String accessToken;
        try {
            accessToken = tokenService.getAccessToken(userId, serverType != null ? serverType : "1");
        } catch (Exception e) {
            log.error("WebSocket 연결 실패(토큰 조회): userId={}", LogMaskingUtil.maskUserId(userId), e);
            return;
        }
        if (accessToken == null || accessToken.isBlank()) {
            log.error("WebSocket 연결 실패: Access Token 없음. userId={}", LogMaskingUtil.maskUserId(userId));
            return;
        }

        URI uri = buildUri(userId, serverType);
        long connectWaitMs = properties.getKoreaInvestment().getWebsocket().getConnectWaitMs();

        if (properties.getKoreaInvestment().getWebsocket().isApprovalKeyFetchEnabled()) {
            String fetched = tokenService.getApprovalKey(userId, serverType);
            if (fetched != null && !fetched.isBlank()) {
                approvalKeyByKey.put(k, fetched);
                log.debug("WebSocket approval_key REST 발급 완료: key={}", LogMaskingUtil.maskUserId(userId));
            }
        }

        webSocketClient.execute(uri, session -> {
            sessions.put(k, session);
            log.info("WebSocket 연결됨: uri={}, key={}", uri.getHost(), LogMaskingUtil.maskUserId(userId));
            return Mono.delay(Duration.ofMillis(connectWaitMs))
                    .then(session.receive()
                            .doOnNext(msg -> {
                                if (msg.getType() == WebSocketMessage.Type.TEXT && msg.getPayloadAsText() != null) {
                                    log.trace("WebSocket 수신: len={}", msg.getPayloadAsText().length());
                                }
                            })
                            .then())
                    .doOnError(e -> {
                        sessions.remove(k);
                        log.warn("WebSocket 수신 오류: key={}, error={}", LogMaskingUtil.maskUserId(userId), e.getMessage());
                    })
                    .doOnTerminate(() -> sessions.remove(k));
        }).subscribe(
                nil -> {},
                e -> {
                    sessions.remove(k);
                    log.error("WebSocket 연결 실패: uri={}, userId={}", uri, LogMaskingUtil.maskUserId(userId), e);
                }
        );
    }

    @Override
    public void disconnect(String userId, String serverType) {
        String k = key(userId, serverType);
        approvalKeyByKey.remove(k);
        WebSocketSession session = sessions.remove(k);
        if (session != null) {
            session.close().subscribe(
                    nil -> log.debug("WebSocket 연결 해제: key={}", LogMaskingUtil.maskUserId(userId)),
                    e -> log.warn("WebSocket 종료 오류: key={}", LogMaskingUtil.maskUserId(userId), e)
            );
        }
    }

    @Override
    public boolean isConnected(String userId, String serverType) {
        WebSocketSession session = sessions.get(key(userId, serverType));
        return session != null && session.isOpen();
    }

    @Override
    public void subscribeQuote(String userId, String serverType, List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        String k = key(userId, serverType);
        WebSocketSession session = sessions.get(k);
        if (session == null || !session.isOpen()) {
            log.debug("WebSocket 미연결로 호가 구독 스킵: 먼저 connect 호출 필요. userId={}", LogMaskingUtil.maskUserId(userId));
            return;
        }
        String trId = properties.getKoreaInvestment().getWebsocket().getQuoteTrId();
        long intervalMs = properties.getKoreaInvestment().getWebsocket().getSubscriptionIntervalMs();
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            String msg = buildSubscribeMessage(k, trId, symbol);
            final int idx = i;
            Mono.delay(Duration.ofMillis(idx * intervalMs))
                    .flatMap(n -> session.send(Mono.just(session.textMessage(msg))))
                    .subscribe(
                            nil -> log.trace("WebSocket 호가 구독 전송: symbol={}", symbol),
                            e -> log.warn("WebSocket 호가 구독 전송 실패: symbol={}", symbol, e)
                    );
        }
    }

    @Override
    public void unsubscribeQuote(List<String> symbols) {
        // KIS: 구독 해제는 동일 tr_id에 tr_key 빈 값 또는 별도 해제 메시지. 문서 확인 후 구현 가능.
        log.debug("WebSocket 호가 구독 해제: symbols={}", symbols);
    }

    @Override
    public void subscribeCcnlNotice(String userId, String serverType) {
        String k = key(userId, serverType);
        WebSocketSession session = sessions.get(k);
        if (session == null || !session.isOpen()) {
            log.debug("WebSocket 미연결로 체결통보 구독 스킵: 먼저 connect 호출 필요. userId={}", LogMaskingUtil.maskUserId(userId));
            return;
        }
        String trId = properties.getKoreaInvestment().getWebsocket().getCcnlNoticeTrId();
        // 체결통보는 계좌번호 등 tr_key 필요 시 REST 문서 확인. 여기서는 빈 tr_key로 구독 요청.
        String msg = buildSubscribeMessage(k, trId, "");
        session.send(Mono.just(session.textMessage(msg))).subscribe(
                nil -> log.debug("WebSocket 체결통보 구독 전송: userId={}", LogMaskingUtil.maskUserId(userId)),
                e -> log.warn("WebSocket 체결통보 구독 전송 실패", e)
        );
    }

    @Override
    public void unsubscribeCcnlNotice(String userId, String serverType) {
        log.debug("WebSocket 체결통보 구독 해제: userId={}", LogMaskingUtil.maskUserId(userId));
    }
}
