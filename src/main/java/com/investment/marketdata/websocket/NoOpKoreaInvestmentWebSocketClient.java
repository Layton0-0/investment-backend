package com.investment.marketdata.websocket;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * WebSocket 실시간 호가·체결통보 미구현 시 사용하는 No-Op 클라이언트.
 * MCP로 asking_price_total, ccnl_notice URL·구독 포맷 확인 후 실제 구현체로 교체.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "investment.market-data.korea-investment.websocket.enabled",
        havingValue = "false",
        matchIfMissing = true)
public class NoOpKoreaInvestmentWebSocketClient implements KoreaInvestmentWebSocketClient {

    @Override
    public void subscribeQuote(String userId, String serverType, List<String> symbols) {
        log.debug("WebSocket 실시간 호가 구독 미구현: userId={}, symbols={}", userId, symbols);
    }

    @Override
    public void unsubscribeQuote(List<String> symbols) {
        log.debug("WebSocket 실시간 호가 구독 해제 미구현: symbols={}", symbols);
    }

    @Override
    public void subscribeCcnlNotice(String userId, String serverType) {
        log.debug("WebSocket 실시간 체결통보 구독 미구현: userId={}, serverType={}", userId, serverType);
    }

    @Override
    public void unsubscribeCcnlNotice(String userId, String serverType) {
        log.debug("WebSocket 실시간 체결통보 구독 해제 미구현: userId={}, serverType={}", userId, serverType);
    }

    @Override
    public void connect(String userId, String serverType) {
        log.debug("WebSocket 연결 미구현: userId={}, serverType={}", userId, serverType);
    }

    @Override
    public void disconnect(String userId, String serverType) {
        log.debug("WebSocket 연결 해제 미구현: userId={}, serverType={}", userId, serverType);
    }

    @Override
    public boolean isConnected(String userId, String serverType) {
        return false;
    }
}
