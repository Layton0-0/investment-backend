package com.investment.marketdata.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.common.security.LogMaskingUtil;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 한국투자증권 실시간 WebSocket 클라이언트 구현체.
 * 실시간 호가(H0STASP0), 실시간 체결(H0STCNT0), 체결통보(H0STCNI0) 구독.
 * 재연결 로직(Exponential Backoff), PINGPONG 하트비트, 메시지 파싱 및 이벤트 발행 지원.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "investment.market-data.korea-investment.websocket.enabled",
        havingValue = "true")
public class KoreaInvestmentWebSocketClientImpl implements KoreaInvestmentWebSocketClient {

    private static final long MIN_CONNECTION_INTERVAL_MS = 1000L;
    private static final String PINGPONG_MESSAGE = "{\"header\":{\"tr_type\":\"9\"}}";

    private final MarketDataProperties properties;
    private final KoreaInvestmentTokenService tokenService;
    private final WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastConnectTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> approvalKeyByKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> reconnectAttempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Disposable> heartbeatDisposables = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> subscribedSymbols = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> ccnlSubscribed = new ConcurrentHashMap<>();

    public KoreaInvestmentWebSocketClientImpl(MarketDataProperties properties,
                                              KoreaInvestmentTokenService tokenService,
                                              ObjectMapper objectMapper,
                                              ApplicationEventPublisher eventPublisher) {
        this.properties = properties;
        this.tokenService = tokenService;
        this.webSocketClient = new ReactorNettyWebSocketClient();
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
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

    private String buildSubscribeMessage(String sessionKey, String trId, String trKey, boolean isSubscribe) {
        try {
            Map<String, Object> header = new java.util.HashMap<>(Map.of(
                    "custtype", "P",
                    "tr_type", isSubscribe ? "1" : "2",
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
        if (sessions.containsKey(k) && sessions.get(k).isOpen()) {
            log.debug("WebSocket 이미 연결됨: key={}", LogMaskingUtil.maskUserId(k.split("\\|")[0]));
            return;
        }
        
        long now = System.currentTimeMillis();
        Long last = lastConnectTime.get(k);
        if (last != null && (now - last) < MIN_CONNECTION_INTERVAL_MS) {
            log.warn("WebSocket 연결 간격 미준수(1초): key={}", LogMaskingUtil.maskUserId(userId));
        }
        lastConnectTime.put(k, now);
        reconnectAttempts.putIfAbsent(k, new AtomicInteger(0));

        String accessToken;
        try {
            accessToken = tokenService.getAccessToken(userId, serverType != null ? serverType : "1");
        } catch (Exception e) {
            log.error("WebSocket 연결 실패(토큰 조회): userId={}", LogMaskingUtil.maskUserId(userId), e);
            scheduleReconnect(userId, serverType);
            return;
        }
        if (accessToken == null || accessToken.isBlank()) {
            log.error("WebSocket 연결 실패: Access Token 없음. userId={}", LogMaskingUtil.maskUserId(userId));
            scheduleReconnect(userId, serverType);
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
            reconnectAttempts.get(k).set(0);
            log.info("WebSocket 연결됨: uri={}, key={}", uri.getHost(), LogMaskingUtil.maskUserId(userId));
            
            startHeartbeat(k, session);
            restoreSubscriptions(k);
            
            return Mono.delay(Duration.ofMillis(connectWaitMs))
                    .then(session.receive()
                            .doOnNext(msg -> handleMessage(k, msg))
                            .then())
                    .doOnError(e -> {
                        stopHeartbeat(k);
                        sessions.remove(k);
                        log.warn("WebSocket 수신 오류: key={}, error={}", LogMaskingUtil.maskUserId(userId), e.getMessage());
                        scheduleReconnect(userId, serverType);
                    })
                    .doOnTerminate(() -> {
                        stopHeartbeat(k);
                        sessions.remove(k);
                        log.info("WebSocket 연결 종료: key={}", LogMaskingUtil.maskUserId(userId));
                    });
        }).subscribe(
                nil -> {},
                e -> {
                    sessions.remove(k);
                    log.error("WebSocket 연결 실패: uri={}, userId={}", uri, LogMaskingUtil.maskUserId(userId), e);
                    scheduleReconnect(userId, serverType);
                }
        );
    }

    private void handleMessage(String sessionKey, WebSocketMessage msg) {
        if (msg.getType() != WebSocketMessage.Type.TEXT) {
            return;
        }
        String payload = msg.getPayloadAsText();
        if (payload == null || payload.isBlank()) {
            return;
        }
        
        try {
            if (payload.contains("PINGPONG")) {
                log.trace("WebSocket PINGPONG 응답 수신");
                return;
            }
            
            JsonNode root = objectMapper.readTree(payload);
            JsonNode header = root.path("header");
            String trId = header.path("tr_id").asText("");
            String trKey = header.path("tr_key").asText("");
            
            if (trId.isEmpty() && payload.contains("|")) {
                parseAndPublishPipedData(sessionKey, payload);
            } else {
                parseAndPublishJsonData(sessionKey, trId, trKey, root);
            }
        } catch (Exception e) {
            if (payload.contains("|")) {
                parseAndPublishPipedData(sessionKey, payload);
            } else {
                log.trace("WebSocket 메시지 파싱 스킵: len={}", payload.length());
            }
        }
    }

    private void parseAndPublishJsonData(String sessionKey, String trId, String trKey, JsonNode root) {
        log.debug("WebSocket JSON 수신: trId={}, trKey={}", trId, trKey);
        
        WebSocketDataEvent event = new WebSocketDataEvent(this, sessionKey, trId, trKey, root.toString());
        eventPublisher.publishEvent(event);
    }

    private void parseAndPublishPipedData(String sessionKey, String payload) {
        String[] parts = payload.split("\\|");
        if (parts.length < 4) {
            log.trace("WebSocket 파이프 데이터 형식 불일치: len={}", payload.length());
            return;
        }
        
        String trId = parts[1];
        String dataCount = parts[2];
        String data = parts[3];
        
        log.debug("WebSocket 파이프 수신: trId={}, dataCount={}", trId, dataCount);
        
        WebSocketDataEvent event = new WebSocketDataEvent(this, sessionKey, trId, "", data);
        eventPublisher.publishEvent(event);
    }

    private void startHeartbeat(String sessionKey, WebSocketSession session) {
        long intervalSeconds = properties.getKoreaInvestment().getWebsocket().getHeartbeatIntervalSeconds();
        if (intervalSeconds <= 0) {
            return;
        }
        
        Disposable heartbeat = Flux.interval(Duration.ofSeconds(intervalSeconds))
                .flatMap(tick -> {
                    if (session.isOpen()) {
                        return session.send(Mono.just(session.textMessage(PINGPONG_MESSAGE)))
                                .doOnSuccess(v -> log.trace("WebSocket PINGPONG 전송"))
                                .onErrorResume(e -> {
                                    log.warn("WebSocket PINGPONG 전송 실패: {}", e.getMessage());
                                    return Mono.empty();
                                });
                    }
                    return Mono.empty();
                })
                .subscribe();
        
        heartbeatDisposables.put(sessionKey, heartbeat);
        log.debug("WebSocket 하트비트 시작: interval={}s, key={}", intervalSeconds, 
                LogMaskingUtil.maskUserId(sessionKey.split("\\|")[0]));
    }

    private void stopHeartbeat(String sessionKey) {
        Disposable heartbeat = heartbeatDisposables.remove(sessionKey);
        if (heartbeat != null && !heartbeat.isDisposed()) {
            heartbeat.dispose();
            log.debug("WebSocket 하트비트 중지: key={}", LogMaskingUtil.maskUserId(sessionKey.split("\\|")[0]));
        }
    }

    private void scheduleReconnect(String userId, String serverType) {
        var wsProps = properties.getKoreaInvestment().getWebsocket();
        if (!wsProps.isReconnectEnabled()) {
            log.debug("WebSocket 재연결 비활성화됨");
            return;
        }
        
        String k = key(userId, serverType);
        AtomicInteger attempts = reconnectAttempts.computeIfAbsent(k, key -> new AtomicInteger(0));
        int currentAttempt = attempts.incrementAndGet();
        int maxAttempts = wsProps.getReconnectMaxAttempts();
        
        if (maxAttempts > 0 && currentAttempt > maxAttempts) {
            log.error("WebSocket 재연결 최대 시도 횟수 초과: userId={}, attempts={}", 
                    LogMaskingUtil.maskUserId(userId), currentAttempt);
            return;
        }
        
        long delay = calculateBackoffDelay(currentAttempt, wsProps);
        log.info("WebSocket 재연결 예약: userId={}, attempt={}, delay={}ms", 
                LogMaskingUtil.maskUserId(userId), currentAttempt, delay);
        
        Mono.delay(Duration.ofMillis(delay))
                .subscribe(tick -> {
                    log.info("WebSocket 재연결 시도: userId={}, attempt={}", 
                            LogMaskingUtil.maskUserId(userId), currentAttempt);
                    connect(userId, serverType);
                });
    }

    private long calculateBackoffDelay(int attempt, MarketDataProperties.KoreaInvestmentProperties.WebSocketProperties wsProps) {
        long initialDelay = wsProps.getReconnectInitialDelayMs();
        long maxDelay = wsProps.getReconnectMaxDelayMs();
        double multiplier = wsProps.getReconnectBackoffMultiplier();
        
        long delay = (long) (initialDelay * Math.pow(multiplier, attempt - 1));
        return Math.min(delay, maxDelay);
    }

    private void restoreSubscriptions(String sessionKey) {
        Set<String> symbols = subscribedSymbols.get(sessionKey);
        if (symbols != null && !symbols.isEmpty()) {
            String[] parts = sessionKey.split("\\|");
            String userId = parts[0];
            String serverType = parts.length > 1 ? parts[1] : "1";
            log.info("WebSocket 구독 복원: symbols={}, userId={}", symbols.size(), LogMaskingUtil.maskUserId(userId));
            subscribeQuote(userId, serverType, List.copyOf(symbols));
        }
        
        Boolean ccnl = ccnlSubscribed.get(sessionKey);
        if (Boolean.TRUE.equals(ccnl)) {
            String[] parts = sessionKey.split("\\|");
            String userId = parts[0];
            String serverType = parts.length > 1 ? parts[1] : "1";
            log.info("WebSocket 체결통보 구독 복원: userId={}", LogMaskingUtil.maskUserId(userId));
            subscribeCcnlNotice(userId, serverType);
        }
    }

    @Override
    public void disconnect(String userId, String serverType) {
        String k = key(userId, serverType);
        reconnectAttempts.remove(k);
        approvalKeyByKey.remove(k);
        subscribedSymbols.remove(k);
        ccnlSubscribed.remove(k);
        stopHeartbeat(k);
        
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
        
        int maxSubs = properties.getKoreaInvestment().getWebsocket().getMaxSubscriptionsPerSession();
        Set<String> existing = subscribedSymbols.computeIfAbsent(k, key -> new CopyOnWriteArraySet<>());
        
        if (existing.size() + symbols.size() > maxSubs) {
            log.warn("WebSocket 구독 한도 초과: current={}, requested={}, max={}", 
                    existing.size(), symbols.size(), maxSubs);
        }
        
        String quoteTrId = properties.getKoreaInvestment().getWebsocket().getQuoteTrId();
        String execTrId = properties.getKoreaInvestment().getWebsocket().getExecutionTrId();
        long intervalMs = properties.getKoreaInvestment().getWebsocket().getSubscriptionIntervalMs();
        
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            if (existing.size() >= maxSubs) {
                log.warn("WebSocket 구독 한도 도달: symbol={} 스킵", symbol);
                break;
            }
            
            existing.add(symbol);
            final int idx = i;
            
            String quoteMsg = buildSubscribeMessage(k, quoteTrId, symbol, true);
            Mono.delay(Duration.ofMillis(idx * intervalMs * 2))
                    .flatMap(n -> session.send(Mono.just(session.textMessage(quoteMsg))))
                    .subscribe(
                            nil -> log.trace("WebSocket 호가 구독 전송: symbol={}", symbol),
                            e -> log.warn("WebSocket 호가 구독 전송 실패: symbol={}", symbol, e)
                    );
            
            String execMsg = buildSubscribeMessage(k, execTrId, symbol, true);
            Mono.delay(Duration.ofMillis(idx * intervalMs * 2 + intervalMs))
                    .flatMap(n -> session.send(Mono.just(session.textMessage(execMsg))))
                    .subscribe(
                            nil -> log.trace("WebSocket 체결 구독 전송: symbol={}", symbol),
                            e -> log.warn("WebSocket 체결 구독 전송 실패: symbol={}", symbol, e)
                    );
        }
    }

    @Override
    public void unsubscribeQuote(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return;
        }
        
        String quoteTrId = properties.getKoreaInvestment().getWebsocket().getQuoteTrId();
        String execTrId = properties.getKoreaInvestment().getWebsocket().getExecutionTrId();
        long intervalMs = properties.getKoreaInvestment().getWebsocket().getSubscriptionIntervalMs();
        
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            String k = entry.getKey();
            WebSocketSession session = entry.getValue();
            
            if (session == null || !session.isOpen()) {
                continue;
            }
            
            Set<String> existing = subscribedSymbols.get(k);
            if (existing == null) {
                continue;
            }
            
            for (int i = 0; i < symbols.size(); i++) {
                String symbol = symbols.get(i);
                if (!existing.contains(symbol)) {
                    continue;
                }
                
                existing.remove(symbol);
                final int idx = i;
                
                String quoteMsg = buildSubscribeMessage(k, quoteTrId, symbol, false);
                Mono.delay(Duration.ofMillis(idx * intervalMs * 2))
                        .flatMap(n -> session.send(Mono.just(session.textMessage(quoteMsg))))
                        .subscribe(
                                nil -> log.trace("WebSocket 호가 구독해제 전송: symbol={}", symbol),
                                e -> log.warn("WebSocket 호가 구독해제 전송 실패: symbol={}", symbol, e)
                        );
                
                String execMsg = buildSubscribeMessage(k, execTrId, symbol, false);
                Mono.delay(Duration.ofMillis(idx * intervalMs * 2 + intervalMs))
                        .flatMap(n -> session.send(Mono.just(session.textMessage(execMsg))))
                        .subscribe(
                                nil -> log.trace("WebSocket 체결 구독해제 전송: symbol={}", symbol),
                                e -> log.warn("WebSocket 체결 구독해제 전송 실패: symbol={}", symbol, e)
                        );
            }
        }
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
        
        ccnlSubscribed.put(k, true);
        String trId = properties.getKoreaInvestment().getWebsocket().getCcnlNoticeTrId();
        String msg = buildSubscribeMessage(k, trId, "", true);
        session.send(Mono.just(session.textMessage(msg))).subscribe(
                nil -> log.debug("WebSocket 체결통보 구독 전송: userId={}", LogMaskingUtil.maskUserId(userId)),
                e -> log.warn("WebSocket 체결통보 구독 전송 실패", e)
        );
    }

    @Override
    public void unsubscribeCcnlNotice(String userId, String serverType) {
        String k = key(userId, serverType);
        ccnlSubscribed.put(k, false);
        
        WebSocketSession session = sessions.get(k);
        if (session == null || !session.isOpen()) {
            return;
        }
        
        String trId = properties.getKoreaInvestment().getWebsocket().getCcnlNoticeTrId();
        String msg = buildSubscribeMessage(k, trId, "", false);
        session.send(Mono.just(session.textMessage(msg))).subscribe(
                nil -> log.debug("WebSocket 체결통보 구독해제 전송: userId={}", LogMaskingUtil.maskUserId(userId)),
                e -> log.warn("WebSocket 체결통보 구독해제 전송 실패", e)
        );
    }

    public int getSubscribedSymbolCount(String userId, String serverType) {
        String k = key(userId, serverType);
        Set<String> symbols = subscribedSymbols.get(k);
        return symbols != null ? symbols.size() : 0;
    }

    @PreDestroy
    public void shutdown() {
        log.info("WebSocket 클라이언트 종료 중...");
        heartbeatDisposables.values().forEach(d -> {
            if (!d.isDisposed()) {
                d.dispose();
            }
        });
        sessions.forEach((k, session) -> {
            if (session.isOpen()) {
                session.close().subscribe();
            }
        });
        sessions.clear();
        heartbeatDisposables.clear();
        subscribedSymbols.clear();
        ccnlSubscribed.clear();
        log.info("WebSocket 클라이언트 종료 완료");
    }
}
