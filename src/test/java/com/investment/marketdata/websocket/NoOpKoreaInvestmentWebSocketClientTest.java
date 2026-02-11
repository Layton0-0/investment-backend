package com.investment.marketdata.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NoOpKoreaInvestmentWebSocketClient")
class NoOpKoreaInvestmentWebSocketClientTest {

    private final NoOpKoreaInvestmentWebSocketClient client = new NoOpKoreaInvestmentWebSocketClient();

    @Test
    @DisplayName("isConnected 항상 false")
    void isConnected_alwaysFalse() {
        assertThat(client.isConnected("user1", "1")).isFalse();
        assertThat(client.isConnected("user1", "0")).isFalse();
    }

    @Test
    @DisplayName("connect / disconnect / subscribe / unsubscribe 예외 없이 동작")
    void allMethods_noThrow() {
        client.connect("user1", "1");
        client.subscribeQuote("user1", "1", List.of("005930"));
        client.subscribeCcnlNotice("user1", "1");
        client.unsubscribeQuote(List.of("005930"));
        client.unsubscribeCcnlNotice("user1", "1");
        client.disconnect("user1", "1");
    }
}
