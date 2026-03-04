package com.investment.alert;

import com.investment.domain.repository.AlertLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordEmergencyAlertService")
class DiscordEmergencyAlertServiceTest {

    private static final String DEFAULT_WEBHOOK = "https://discord.com/api/webhooks/default";
    private static final String TRADE_WEBHOOK = "https://discord.com/api/webhooks/trade";
    private static final String RISK_WEBHOOK = "https://discord.com/api/webhooks/risk";
    private static final String SYSTEM_WEBHOOK = "https://discord.com/api/webhooks/system";

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private AlertLogRepository alertLogRepository;

    private final AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();

    private DiscordEmergencyAlertService service;

    @BeforeEach
    void setUp() {
        capturedRequest.set(null);
        ExchangeFunction exchangeFunction = request -> {
            capturedRequest.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK).build());
        };
        WebClient webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        lenient().when(webClientBuilder.build()).thenReturn(webClient);

        service = new DiscordEmergencyAlertService(webClientBuilder, alertLogRepository);
        ReflectionTestUtils.setField(service, "defaultWebhookUrl", DEFAULT_WEBHOOK);
        ReflectionTestUtils.setField(service, "tradeWebhookUrl", "");
        ReflectionTestUtils.setField(service, "riskWebhookUrl", "");
        ReflectionTestUtils.setField(service, "systemWebhookUrl", "");
    }

    @Test
    @DisplayName("웹훅 미설정 시 sendTradeAlert는 발송 스킵")
    void sendTradeAlert_noWebhook_skips() {
        ReflectionTestUtils.setField(service, "defaultWebhookUrl", "");
        ReflectionTestUtils.setField(service, "tradeWebhookUrl", "");

        service.sendTradeAlert("005930", 10, "매수", "1.5");

        assertThat(capturedRequest.get()).isNull();
    }

    @Test
    @DisplayName("매매 알림 발송 시 기본 웹훅 사용 및 요청 1회 발생")
    void sendTradeAlert_sendsToDefaultWhenNoTradeWebhook() {
        service.sendTradeAlert("005930", 10, "매수", "1.5");

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(DEFAULT_WEBHOOK);
    }

    @Test
    @DisplayName("채널별 웹훅 설정 시 해당 URL로 발송 - TRADE")
    void sendTradeAlert_usesTradeWebhookWhenSet() {
        ReflectionTestUtils.setField(service, "tradeWebhookUrl", TRADE_WEBHOOK);

        service.sendTradeAlert("005930", 1, "매도", null);

        assertThat(capturedRequest.get().url().toString()).isEqualTo(TRADE_WEBHOOK);
    }

    @Test
    @DisplayName("채널별 웹훅 설정 시 해당 URL로 발송 - RISK")
    void sendRiskEventAlert_usesRiskWebhookWhenSet() {
        ReflectionTestUtils.setField(service, "riskWebhookUrl", RISK_WEBHOOK);

        service.sendRiskEventAlert("WARNING", "VarExceeded", "VaR 95% 초과 알림");

        assertThat(capturedRequest.get().url().toString()).isEqualTo(RISK_WEBHOOK);
    }

    @Test
    @DisplayName("채널별 웹훅 설정 시 해당 URL로 발송 - SYSTEM(미체결)")
    void sendUnfilledAlert_usesSystemWebhookWhenSet() {
        ReflectionTestUtils.setField(service, "systemWebhookUrl", SYSTEM_WEBHOOK);

        service.sendUnfilledAlert("ord-1", "005930", 5, 10,
                "user1", "12345678", "1", "한국투자증권", null);

        assertThat(capturedRequest.get().url().toString()).isEqualTo(SYSTEM_WEBHOOK);
    }

    @Test
    @DisplayName("채널 웹훅 미설정 시 기본 webhook 폴백")
    void sendRiskEventAlert_fallbackToDefaultWhenChannelNotSet() {
        service.sendRiskEventAlert("WARNING", "MDD", "MDD -10% 초과");

        assertThat(capturedRequest.get().url().toString()).isEqualTo(DEFAULT_WEBHOOK);
    }

    @Test
    @DisplayName("sendTestAlert는 기본 채널로 발송")
    void sendTestAlert_usesDefaultChannel() {
        boolean result = service.sendTestAlert();

        assertThat(result).isTrue();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(DEFAULT_WEBHOOK);
    }
}
