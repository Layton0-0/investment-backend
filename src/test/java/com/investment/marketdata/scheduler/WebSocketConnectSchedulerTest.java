package com.investment.marketdata.scheduler;

import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.websocket.KoreaInvestmentWebSocketClient;
import com.investment.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.TaskScheduler;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebSocketConnectScheduler")
class WebSocketConnectSchedulerTest {

    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private UserApiKeyRepository userApiKeyRepository;
    @Mock
    private MarketDataProperties marketDataProperties;
    @Mock
    private ObjectProvider<KoreaInvestmentWebSocketClient> webSocketClientProvider;
    @Mock
    private KoreaInvestmentWebSocketClient webSocketClient;
    @Mock
    private SystemSettingService systemSettingService;

    private WebSocketConnectScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new WebSocketConnectScheduler(
                taskScheduler,
                userApiKeyRepository,
                marketDataProperties,
                webSocketClientProvider,
                systemSettingService
        );
    }

    @Test
    @DisplayName("runConnect 시 KOREA_INVESTMENT 건수만큼 connect 호출")
    void runConnect_callsConnectPerKoreaInvestmentKey() {
        UserApiKey key1 = UserApiKey.builder()
                .userId("user1")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType("1")
                .appKeyEncrypted("enc")
                .appSecretEncrypted("enc")
                .build();
        UserApiKey key2 = UserApiKey.builder()
                .userId("user2")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType("0")
                .appKeyEncrypted("enc")
                .appSecretEncrypted("enc")
                .build();
        when(userApiKeyRepository.findAll()).thenReturn(List.of(key1, key2));
        when(webSocketClientProvider.getIfAvailable()).thenReturn(webSocketClient);

        scheduler.runConnect();

        verify(userApiKeyRepository, times(1)).findAll();
        verify(webSocketClient, times(1)).connect(eq("user1"), eq("1"));
        verify(webSocketClient, times(1)).connect(eq("user2"), eq("0"));
    }

    @Test
    @DisplayName("runConnect 시 client 없으면 connect 미호출")
    void runConnect_clientUnavailable_skipsConnect() {
        when(webSocketClientProvider.getIfAvailable()).thenReturn(null);

        scheduler.runConnect();

        verify(userApiKeyRepository, never()).findAll();
        verify(webSocketClient, never()).connect(any(), any());
    }

    @Test
    @DisplayName("runConnect 시 KOREA_INVESTMENT 키 없으면 connect 미호출")
    void runConnect_noKoreaInvestmentKeys_skipsConnect() {
        when(webSocketClientProvider.getIfAvailable()).thenReturn(webSocketClient);
        when(userApiKeyRepository.findAll()).thenReturn(List.of());

        scheduler.runConnect();

        verify(webSocketClient, never()).connect(any(), any());
    }

    @Test
    @DisplayName("runConnect 시 다른 brokerType 키는 connect 미호출")
    void runConnect_filtersOnlyKoreaInvestment() {
        UserApiKey koreaKey = UserApiKey.builder()
                .userId("user1")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType("1")
                .appKeyEncrypted("enc")
                .appSecretEncrypted("enc")
                .build();
        when(userApiKeyRepository.findAll()).thenReturn(List.of(koreaKey));
        when(webSocketClientProvider.getIfAvailable()).thenReturn(webSocketClient);

        scheduler.runConnect();

        verify(webSocketClient, times(1)).connect(eq("user1"), eq("1"));
    }

    @Test
    @DisplayName("runDisconnect 시 KOREA_INVESTMENT 건수만큼 disconnect 호출")
    void runDisconnect_callsDisconnectPerKoreaInvestmentKey() {
        UserApiKey key1 = UserApiKey.builder()
                .userId("user1")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType("1")
                .appKeyEncrypted("enc")
                .appSecretEncrypted("enc")
                .build();
        when(userApiKeyRepository.findAll()).thenReturn(List.of(key1));
        when(webSocketClientProvider.getIfAvailable()).thenReturn(webSocketClient);

        scheduler.runDisconnect();

        verify(userApiKeyRepository, times(1)).findAll();
        verify(webSocketClient, times(1)).disconnect(eq("user1"), eq("1"));
    }
}
