package com.investment.marketdata.websocket;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * WebSocket으로 수신된 실시간 데이터 이벤트.
 * ApplicationEventPublisher를 통해 발행되어 리스너에서 처리.
 */
@Getter
public class WebSocketDataEvent extends ApplicationEvent {

    private final String sessionKey;
    private final String trId;
    private final String trKey;
    private final String data;

    public WebSocketDataEvent(Object source, String sessionKey, String trId, String trKey, String data) {
        super(source);
        this.sessionKey = sessionKey;
        this.trId = trId;
        this.trKey = trKey;
        this.data = data;
    }

    public String getUserId() {
        if (sessionKey == null || !sessionKey.contains("|")) {
            return "";
        }
        return sessionKey.split("\\|")[0];
    }

    public String getServerType() {
        if (sessionKey == null || !sessionKey.contains("|")) {
            return "1";
        }
        String[] parts = sessionKey.split("\\|");
        return parts.length > 1 ? parts[1] : "1";
    }

    public boolean isQuoteData() {
        return "H0STASP0".equals(trId);
    }

    public boolean isExecutionData() {
        return "H0STCNT0".equals(trId);
    }

    public boolean isCcnlNotice() {
        return "H0STCNI0".equals(trId);
    }
}
