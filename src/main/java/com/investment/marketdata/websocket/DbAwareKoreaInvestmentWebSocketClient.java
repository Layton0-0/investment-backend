package com.investment.marketdata.websocket;

import com.investment.setting.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DB 시스템 설정(marketData.websocketEnabled)에 따라 실제 WebSocket 클라이언트로 위임하거나 no-op.
 * 설정이 true일 때만 Impl에 위임.
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class DbAwareKoreaInvestmentWebSocketClient implements KoreaInvestmentWebSocketClient {

    private static final String WEBSOCKET_ENABLED_KEY = "marketData.websocketEnabled";

    private final SystemSettingService systemSettingService;
    @Qualifier("koreaInvestmentWebSocketClientImpl")
    private final KoreaInvestmentWebSocketClient delegate;

    private boolean isEnabled() {
        try {
            return Boolean.TRUE.equals(systemSettingService.getBoolean(WEBSOCKET_ENABLED_KEY));
        } catch (Exception e) {
            log.trace("WebSocket 설정 조회 실패, 비활성 처리: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void subscribeQuote(String userId, String serverType, List<String> symbols) {
        if (isEnabled()) {
            delegate.subscribeQuote(userId, serverType, symbols);
        }
    }

    @Override
    public void unsubscribeQuote(List<String> symbols) {
        if (isEnabled()) {
            delegate.unsubscribeQuote(symbols);
        }
    }

    @Override
    public void subscribeCcnlNotice(String userId, String serverType) {
        if (isEnabled()) {
            delegate.subscribeCcnlNotice(userId, serverType);
        }
    }

    @Override
    public void unsubscribeCcnlNotice(String userId, String serverType) {
        if (isEnabled()) {
            delegate.unsubscribeCcnlNotice(userId, serverType);
        }
    }

    @Override
    public void connect(String userId, String serverType) {
        if (isEnabled()) {
            delegate.connect(userId, serverType);
        }
    }

    @Override
    public void disconnect(String userId, String serverType) {
        if (isEnabled()) {
            delegate.disconnect(userId, serverType);
        }
    }

    @Override
    public boolean isConnected(String userId, String serverType) {
        return isEnabled() && delegate.isConnected(userId, serverType);
    }
}
