package com.investment.marketdata.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KoreaInvestmentWebSocketClientImpl")
class KoreaInvestmentWebSocketClientImplTest {

    private static final String USER_ID = "user-1";
    private static final String SERVER_TYPE = "1";

    @Mock
    private MarketDataProperties properties;
    @Mock
    private MarketDataProperties.KoreaInvestmentProperties koreaInvestment;
    @Mock
    private MarketDataProperties.KoreaInvestmentProperties.WebSocketProperties websocket;
    @Mock
    private KoreaInvestmentTokenService tokenService;

    private KoreaInvestmentWebSocketClientImpl client;

    @BeforeEach
    void setUp() {
        when(properties.getKoreaInvestment()).thenReturn(koreaInvestment);
        when(koreaInvestment.getWebsocket()).thenReturn(websocket);
        when(websocket.getBaseUrlReal()).thenReturn("wss://openapi.koreainvestment.com:9443");
        when(websocket.getBaseUrlVirtual()).thenReturn("wss://openapivts.koreainvestment.com:29443");
        when(websocket.getPath()).thenReturn("/tryitout");
        when(websocket.getQuoteTrId()).thenReturn("H0GASP0");
        when(websocket.getCcnlNoticeTrId()).thenReturn("H0GAMT0");
        when(websocket.getConnectWaitMs()).thenReturn(1000L);
        when(websocket.getSubscriptionIntervalMs()).thenReturn(200L);
        when(websocket.getApprovalKey()).thenReturn("");
        when(websocket.isApprovalKeyFetchEnabled()).thenReturn(false);

        client = new KoreaInvestmentWebSocketClientImpl(properties, tokenService, new ObjectMapper());
    }

    @Test
    @DisplayName("토큰 조회 실패 시 connect 후 isConnected false")
    void connect_whenTokenFails_thenIsConnectedFalse() {
        when(tokenService.getAccessToken(USER_ID, SERVER_TYPE))
                .thenThrow(new RuntimeException("토큰 없음"));

        client.connect(USER_ID, SERVER_TYPE);

        assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
    }

    @Test
    @DisplayName("subscribeQuote 빈 목록이면 no-op")
    void subscribeQuote_emptyList_doesNothing() {
        client.subscribeQuote(USER_ID, SERVER_TYPE, List.of());
        assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
    }

    @Test
    @DisplayName("disconnect 세션 없으면 no-op")
    void disconnect_whenNoSession_doesNotThrow() {
        client.disconnect(USER_ID, SERVER_TYPE);
    }
}
