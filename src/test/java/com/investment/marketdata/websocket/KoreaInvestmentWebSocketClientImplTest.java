package com.investment.marketdata.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private KoreaInvestmentWebSocketClientImpl client;

    @BeforeEach
    void setUp() {
        when(properties.getKoreaInvestment()).thenReturn(koreaInvestment);
        when(koreaInvestment.getWebsocket()).thenReturn(websocket);
        when(websocket.getBaseUrlReal()).thenReturn("wss://openapi.koreainvestment.com:9443");
        when(websocket.getBaseUrlVirtual()).thenReturn("wss://openapivts.koreainvestment.com:29443");
        when(websocket.getPath()).thenReturn("/tryitout");
        when(websocket.getQuoteTrId()).thenReturn("H0STASP0");
        when(websocket.getExecutionTrId()).thenReturn("H0STCNT0");
        when(websocket.getCcnlNoticeTrId()).thenReturn("H0STCNI0");
        when(websocket.getConnectWaitMs()).thenReturn(1000L);
        when(websocket.getSubscriptionIntervalMs()).thenReturn(200L);
        when(websocket.getApprovalKey()).thenReturn("");
        when(websocket.isApprovalKeyFetchEnabled()).thenReturn(false);
        when(websocket.isReconnectEnabled()).thenReturn(true);
        when(websocket.getReconnectMaxAttempts()).thenReturn(3);
        when(websocket.getReconnectInitialDelayMs()).thenReturn(1000L);
        when(websocket.getReconnectMaxDelayMs()).thenReturn(10000L);
        when(websocket.getReconnectBackoffMultiplier()).thenReturn(2.0);
        when(websocket.getHeartbeatIntervalSeconds()).thenReturn(30L);
        when(websocket.getMaxSubscriptionsPerSession()).thenReturn(41);

        client = new KoreaInvestmentWebSocketClientImpl(
                properties, tokenService, new ObjectMapper(), eventPublisher);
    }

    @Nested
    @DisplayName("connect")
    class ConnectTests {

        @Test
        @DisplayName("토큰 조회 실패 시 connect 후 isConnected false")
        void connect_whenTokenFails_thenIsConnectedFalse() {
            when(tokenService.getAccessToken(USER_ID, SERVER_TYPE))
                    .thenThrow(new RuntimeException("토큰 없음"));

            client.connect(USER_ID, SERVER_TYPE);

            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }

        @Test
        @DisplayName("토큰 null 반환 시 isConnected false")
        void connect_whenTokenNull_thenIsConnectedFalse() {
            when(tokenService.getAccessToken(USER_ID, SERVER_TYPE)).thenReturn(null);

            client.connect(USER_ID, SERVER_TYPE);

            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }

        @Test
        @DisplayName("토큰 빈 문자열 시 isConnected false")
        void connect_whenTokenEmpty_thenIsConnectedFalse() {
            when(tokenService.getAccessToken(USER_ID, SERVER_TYPE)).thenReturn("");

            client.connect(USER_ID, SERVER_TYPE);

            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }
    }

    @Nested
    @DisplayName("subscribeQuote")
    class SubscribeQuoteTests {

        @Test
        @DisplayName("빈 목록이면 no-op")
        void subscribeQuote_emptyList_doesNothing() {
            client.subscribeQuote(USER_ID, SERVER_TYPE, List.of());
            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }

        @Test
        @DisplayName("null 목록이면 no-op")
        void subscribeQuote_nullList_doesNothing() {
            client.subscribeQuote(USER_ID, SERVER_TYPE, null);
            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }

        @Test
        @DisplayName("미연결 상태에서 구독 시도시 스킵")
        void subscribeQuote_whenNotConnected_skips() {
            client.subscribeQuote(USER_ID, SERVER_TYPE, List.of("005930"));
            assertThat(client.getSubscribedSymbolCount(USER_ID, SERVER_TYPE)).isZero();
        }
    }

    @Nested
    @DisplayName("unsubscribeQuote")
    class UnsubscribeQuoteTests {

        @Test
        @DisplayName("빈 목록이면 no-op")
        void unsubscribeQuote_emptyList_doesNothing() {
            client.unsubscribeQuote(List.of());
        }

        @Test
        @DisplayName("null 목록이면 no-op")
        void unsubscribeQuote_nullList_doesNothing() {
            client.unsubscribeQuote(null);
        }
    }

    @Nested
    @DisplayName("subscribeCcnlNotice")
    class SubscribeCcnlNoticeTests {

        @Test
        @DisplayName("미연결 상태에서 체결통보 구독 시도시 스킵")
        void subscribeCcnlNotice_whenNotConnected_skips() {
            client.subscribeCcnlNotice(USER_ID, SERVER_TYPE);
            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }
    }

    @Nested
    @DisplayName("disconnect")
    class DisconnectTests {

        @Test
        @DisplayName("세션 없으면 no-op")
        void disconnect_whenNoSession_doesNotThrow() {
            client.disconnect(USER_ID, SERVER_TYPE);
        }

        @Test
        @DisplayName("disconnect 후 isConnected false")
        void disconnect_thenIsConnectedFalse() {
            client.disconnect(USER_ID, SERVER_TYPE);
            assertThat(client.isConnected(USER_ID, SERVER_TYPE)).isFalse();
        }
    }

    @Nested
    @DisplayName("shutdown")
    class ShutdownTests {

        @Test
        @DisplayName("shutdown 호출 시 예외 없음")
        void shutdown_doesNotThrow() {
            client.shutdown();
        }
    }

    @Nested
    @DisplayName("getSubscribedSymbolCount")
    class GetSubscribedSymbolCountTests {

        @Test
        @DisplayName("구독 전 0 반환")
        void getSubscribedSymbolCount_beforeSubscribe_returnsZero() {
            assertThat(client.getSubscribedSymbolCount(USER_ID, SERVER_TYPE)).isZero();
        }
    }
}
