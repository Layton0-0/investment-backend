package com.investment.alert;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.AlertLog;
import com.investment.domain.repository.AlertLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Discord Incoming Webhook을 이용한 긴급 알림 발송.
 * 매매/리스크/시스템 채널 분리: trade-webhook, risk-webhook, system-webhook 미설정 시 기본 webhook 폴백.
 * 알림 본문에 userId, 계좌(마스킹), 모의/실계좌 여부, 증권사, URL 포함.
 */
@Slf4j
@Service
public class DiscordEmergencyAlertService implements EmergencyAlertService {

    private static final String BROKER_DEFAULT = "한국투자증권";

    public enum ChannelType { TRADE, RISK, SYSTEM }

    @Value("${investment.pipeline.alert-discord-webhook-url:}")
    private String defaultWebhookUrl;

    @Value("${investment.alert.discord.trade-webhook:}")
    private String tradeWebhookUrl;

    @Value("${investment.alert.discord.risk-webhook:}")
    private String riskWebhookUrl;

    @Value("${investment.alert.discord.system-webhook:}")
    private String systemWebhookUrl;

    private final WebClient.Builder webClientBuilder;
    private final AlertLogRepository alertLogRepository;

    public DiscordEmergencyAlertService(WebClient.Builder webClientBuilder,
                                        AlertLogRepository alertLogRepository) {
        this.webClientBuilder = webClientBuilder;
        this.alertLogRepository = alertLogRepository;
    }

    private String resolveWebhookUrl(ChannelType type) {
        String url = switch (type) {
            case TRADE -> tradeWebhookUrl;
            case RISK -> riskWebhookUrl;
            case SYSTEM -> systemWebhookUrl;
        };
        return (url != null && !url.isBlank()) ? url : defaultWebhookUrl;
    }

    @Override
    public void sendUnfilledAlert(String orderId, String symbol, int outstandingQty, int elapsedMin,
            String userId, String accountNo, String serverType, String broker, String baseUrl) {
        String webhookUrl = resolveWebhookUrl(ChannelType.SYSTEM);
        if (webhookUrl == null || webhookUrl.isBlank()) {
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
        String content = body.toString();
        persistAlert("WARNING", "UnfilledOrder", content);
        sendToDiscord(content, ChannelType.SYSTEM);
    }

    @Override
    public void sendFailureAlert(String title, String message, String userId, String accountNo,
            String serverType, String broker, String baseUrl) {
        String webhookUrl = resolveWebhookUrl(ChannelType.SYSTEM);
        if (webhookUrl == null || webhookUrl.isBlank()) {
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
        String content = body.toString();
        persistAlert("ERROR", "Failure", content);
        sendToDiscord(content, ChannelType.SYSTEM);
    }

    @Override
    public void sendTradeAlert(String symbol, int quantity, String side, String pnlPct) {
        String webhookUrl = resolveWebhookUrl(ChannelType.TRADE);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Discord 매매 Webhook URL 미설정, 매매 알림 스킵");
            return;
        }
        String content = String.format("**매매 체결** %s %d주 %s 완료. 수익률: %s%%.", symbol, quantity, side, pnlPct != null ? pnlPct : "-");
        persistAlert("INFO", "Trade", content);
        sendToDiscord(content, ChannelType.TRADE);
    }

    @Override
    public void sendRiskEventAlert(String level, String component, String message) {
        String webhookUrl = resolveWebhookUrl(ChannelType.RISK);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Discord Webhook URL 미설정, 리스크 이벤트 알림 스킵");
            return;
        }
        String safeLevel = (level != null && !level.isBlank()) ? level : "WARNING";
        String safeComponent = (component != null && !component.isBlank()) ? component : "RiskEvent";
        persistAlert(safeLevel, safeComponent, message);
        sendToDiscord(message, ChannelType.RISK);
    }

    @Override
    public boolean sendTestAlert() {
        String webhookUrl = resolveWebhookUrl(ChannelType.SYSTEM);
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Discord Webhook URL 미설정, 테스트 알림 스킵");
            return false;
        }
        String message = "**[테스트]** Discord 웹훅 연결이 정상입니다. (Investment Backend)";
        try {
            sendToDiscord(message, ChannelType.SYSTEM);
            return true;
        } catch (Exception e) {
            log.warn("Discord 테스트 알림 발송 실패: {}", e.getMessage());
            return false;
        }
    }

    private void persistAlert(String level, String component, String message) {
        try {
            alertLogRepository.save(AlertLog.of(Instant.now(), level, component, message));
        } catch (Exception e) {
            log.warn("알림 이력 저장 실패: {}", e.getMessage());
        }
    }

    private void sendToDiscord(String content, ChannelType channelType) {
        String url = resolveWebhookUrl(channelType);
        if (url == null || url.isBlank()) {
            log.debug("Discord Webhook URL 없음, 채널={}", channelType);
            return;
        }
        try {
            log.info("외부 API 호출: system=Discord, api=긴급알림, url={}, method=POST, channel={}", url, channelType);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", content);
            webClientBuilder.build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Discord 알림 발송 완료, 채널={}", channelType);
        } catch (Exception e) {
            log.warn("Discord 알림 발송 실패, 채널={}: {}", channelType, e.getMessage());
        }
    }
}
