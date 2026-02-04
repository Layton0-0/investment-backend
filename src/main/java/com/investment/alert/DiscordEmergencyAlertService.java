package com.investment.alert;

import com.investment.common.security.LogMaskingUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discord Incoming Webhook을 이용한 긴급 알림 발송.
 * 알림 본문에 userId, 계좌(마스킹), 모의/실계좌 여부, 증권사, URL 포함.
 */
@Slf4j
@Service
public class DiscordEmergencyAlertService implements EmergencyAlertService {

    private static final String BROKER_DEFAULT = "한국투자증권";

    @Value("${investment.pipeline.alert-discord-webhook-url:}")
    private String discordWebhookUrl;

    private final WebClient.Builder webClientBuilder;

    public DiscordEmergencyAlertService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public void sendUnfilledAlert(String orderId, String symbol, int outstandingQty, int elapsedMin,
            String userId, String accountNo, String serverType, String broker, String baseUrl) {
        if (discordWebhookUrl == null || discordWebhookUrl.isBlank()) {
            log.debug("Discord Webhook URL 미설정, 긴급 알림 스킵");
            return;
        }
        String accountMasked = LogMaskingUtil.maskAccountNo(accountNo);
        String userIdMasked = LogMaskingUtil.maskUserId(userId);
        String serverLabel = "1".equals(serverType) ? "모의" : "실전";
        String brokerName = broker != null && !broker.isBlank() ? broker : BROKER_DEFAULT;
        StringBuilder body = new StringBuilder();
        body.append("** [긴급] 미체결 주문 알림 **\n");
        body.append("주문ID: ").append(orderId).append("\n");
        body.append("종목: ").append(symbol).append("\n");
        body.append("미체결 수량: ").append(outstandingQty).append("\n");
        body.append("경과(분): ").append(elapsedMin).append("\n");
        body.append("사용자ID: ").append(userIdMasked).append("\n");
        body.append("계좌: ").append(accountMasked).append("\n");
        body.append("모의/실전: ").append(serverLabel).append("\n");
        body.append("증권사: ").append(brokerName).append("\n");
        if (baseUrl != null && !baseUrl.isBlank()) {
            body.append("URL: ").append(baseUrl.trim()).append("\n");
        }
        sendToDiscord(body.toString());
    }

    @Override
    public void sendFailureAlert(String title, String message, String userId, String accountNo,
            String serverType, String broker, String baseUrl) {
        if (discordWebhookUrl == null || discordWebhookUrl.isBlank()) {
            log.debug("Discord Webhook URL 미설정, 실패 알림 스킵");
            return;
        }
        String accountMasked = LogMaskingUtil.maskAccountNo(accountNo);
        String userIdMasked = LogMaskingUtil.maskUserId(userId);
        String serverLabel = "1".equals(serverType) ? "모의" : "실전";
        String brokerName = broker != null && !broker.isBlank() ? broker : BROKER_DEFAULT;
        StringBuilder body = new StringBuilder();
        body.append("** ").append(title != null ? title : "실패 알림").append(" **\n");
        body.append(message).append("\n");
        body.append("사용자ID: ").append(userIdMasked).append("\n");
        body.append("계좌: ").append(accountMasked).append("\n");
        body.append("모의/실전: ").append(serverLabel).append("\n");
        body.append("증권사: ").append(brokerName).append("\n");
        if (baseUrl != null && !baseUrl.isBlank()) {
            body.append("URL: ").append(baseUrl.trim()).append("\n");
        }
        sendToDiscord(body.toString());
    }

    private void sendToDiscord(String content) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", content);
            webClientBuilder.build()
                    .post()
                    .uri(discordWebhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Discord 긴급 알림 발송 완료");
        } catch (Exception e) {
            log.warn("Discord 긴급 알림 발송 실패: {}", e.getMessage());
        }
    }
}
