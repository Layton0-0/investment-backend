package com.investment.alert;

import com.investment.domain.entity.AlertLog;
import com.investment.domain.repository.AlertLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Discord 알림 체계화(P6-4): 매매/리스크/시스템 채널 분리, 평문 형식, 미설정 시 기본 웹훅 폴백 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscordEmergencyAlertService")
class DiscordEmergencyAlertServiceTest {

    private static final String TRADE_URL = "https://discord.com/api/webhooks/trade/xxx";
    private static final String RISK_URL = "https://discord.com/api/webhooks/risk/xxx";
    private static final String SYSTEM_URL = "https://discord.com/api/webhooks/system/xxx";
    private static final String DEFAULT_URL = "https://discord.com/api/webhooks/default/xxx";

    @Mock
    private AlertLogRepository alertLogRepository;

    private final AtomicReference<ClientRequest> capturedRequest = new AtomicReference<>();

    private DiscordEmergencyAlertService service;

    @BeforeEach
    void setUp() {
        ExchangeFunction okResponse = request -> {
            capturedRequest.set(request);
            return reactor.core.publisher.Mono.just(
                    ClientResponse.create(HttpStatus.OK).build()
            );
        };
        WebClient webClient = WebClient.builder().exchangeFunction(okResponse).build();
        WebClient.Builder builder = org.mockito.Mockito.mock(WebClient.Builder.class);
        lenient().when(builder.build()).thenReturn(webClient);
        service = new DiscordEmergencyAlertService(builder, alertLogRepository);
    }

    private void setWebhookUrls(String defaultUrl, String tradeUrl, String riskUrl, String systemUrl) {
        ReflectionTestUtils.setField(service, "defaultWebhookUrl", defaultUrl != null ? defaultUrl : "");
        ReflectionTestUtils.setField(service, "tradeWebhookUrl", tradeUrl != null ? tradeUrl : "");
        ReflectionTestUtils.setField(service, "riskWebhookUrl", riskUrl != null ? riskUrl : "");
        ReflectionTestUtils.setField(service, "systemWebhookUrl", systemUrl != null ? systemUrl : "");
    }

    @Test
    @DisplayName("매매 알림은 trade-webhook URL로 발송된다")
    void sendTradeAlert_usesTradeWebhook() {
        setWebhookUrls(DEFAULT_URL, TRADE_URL, RISK_URL, SYSTEM_URL);
        capturedRequest.set(null);

        service.sendTradeAlert("005930", 10, "매수", "1.5");

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(TRADE_URL);
    }

    @Test
    @DisplayName("리스크 알림은 risk-webhook URL로 발송된다")
    void sendRiskEventAlert_usesRiskWebhook() {
        setWebhookUrls(DEFAULT_URL, TRADE_URL, RISK_URL, SYSTEM_URL);
        capturedRequest.set(null);

        service.sendRiskEventAlert("WARNING", "DailyLoss", "일일 손실 한도 80% 도달");

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(RISK_URL);
    }

    @Test
    @DisplayName("시스템 알림(실패)은 system-webhook URL로 발송된다")
    void sendFailureAlert_usesSystemWebhook() {
        setWebhookUrls(DEFAULT_URL, TRADE_URL, RISK_URL, SYSTEM_URL);
        capturedRequest.set(null);

        service.sendFailureAlert("수집 실패", "KRX 일봉 수집 실패", "user1", "12345", "1", "한국투자증권", null);

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(SYSTEM_URL);
    }

    @Test
    @DisplayName("trade-webhook 미설정 시 기본 webhook으로 폴백한다")
    void sendTradeAlert_fallbackToDefaultWhenTradeWebhookEmpty() {
        setWebhookUrls(DEFAULT_URL, "", "", "");
        capturedRequest.set(null);

        service.sendTradeAlert("005930", 5, "매도", "2.3");

        assertThat(capturedRequest.get()).isNotNull();
        assertThat(capturedRequest.get().url().toString()).isEqualTo(DEFAULT_URL);
    }

    @Test
    @DisplayName("매매 알림 본문은 초보자 친화 평문 형식이다")
    void sendTradeAlert_plainTextFormat() {
        setWebhookUrls(DEFAULT_URL, TRADE_URL, RISK_URL, SYSTEM_URL);
        ArgumentCaptor<AlertLog> logCaptor = ArgumentCaptor.forClass(AlertLog.class);

        service.sendTradeAlert("005930", 10, "매수", "1.5");

        verify(alertLogRepository).save(logCaptor.capture());
        String message = logCaptor.getValue().getMessage();
        assertThat(message).contains("매매 체결");
        assertThat(message).contains("005930");
        assertThat(message).contains("10");
        assertThat(message).contains("매수");
        assertThat(message).contains("1.5");
    }

    @Test
    @DisplayName("웹훅 미설정 시 매매 알림은 스킵하고 예외를 던지지 않는다")
    void sendTradeAlert_skipsWhenNoWebhook() {
        setWebhookUrls("", "", "", "");
        capturedRequest.set(null);

        service.sendTradeAlert("005930", 10, "매수", null);

        assertThat(capturedRequest.get()).isNull();
    }
}
