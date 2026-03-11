package com.investment.marketdata.service;

import com.investment.common.logging.KoreaInvestmentApiLogging;
import com.investment.common.security.LogMaskingUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * 한국투자증권 토큰 발급 클라이언트
 * 
 * 사용자별 API 키로 토큰을 발급합니다.
 */
@Slf4j
@Component
public class KoreaInvestmentTokenClient {

    private static final String BASE_URL_REAL = "https://openapi.koreainvestment.com:9443";
    private static final String BASE_URL_VIRTUAL = "https://openapivts.koreainvestment.com:29443";

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public KoreaInvestmentTokenClient(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Access Token 발급
     * 
     * @param appKey     App Key
     * @param appSecret  App Secret
     * @param serverType "1": 모의투자, "0": 실거래
     * @return Access Token
     */
    public Mono<String> issueAccessToken(String appKey, String appSecret, String serverType) {
        // 입력값 검증
        if (appKey == null || appKey.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("App Key가 비어있습니다"));
        }
        if (appSecret == null || appSecret.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("App Secret이 비어있습니다"));
        }
        if (serverType == null || (!serverType.equals("0") && !serverType.equals("1"))) {
            return Mono.error(new IllegalArgumentException("서버 타입이 올바르지 않습니다 (0: 실거래, 1: 모의투자)"));
        }

        String baseUrl = "1".equals(serverType) ? BASE_URL_VIRTUAL : BASE_URL_REAL;

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/oauth2/tokenP")
                .build()
                .toUri();

        KoreaInvestmentApiLogging.logApiCallInfo("한국투자증권", "접근토큰발급", uri.toString(), "POST");
        KoreaInvestmentApiLogging.logRequest("접근토큰발급", "/oauth2/tokenP", null, java.util.List.of("grant_type", "appkey", "appsecret"));
        LogMaskingUtil.logWithDebugActualAtDebug(log,
                "한국투자증권 Access Token 발급 요청: baseUrl={}, appKey={}",
                new Object[] { baseUrl, LogMaskingUtil.maskApiKey(appKey) },
                "appKey(actual)={}", appKey);

        // 요청 바디 생성 (JSON 형식)
        Map<String, String> requestBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret);

        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(Map.class)
                                .map(body -> {
                                    @SuppressWarnings("unchecked")
                                    String token = (String) ((Map<String, Object>) body).get("access_token");
                                    if (token == null) {
                                        log.error("Access Token 응답에 access_token이 없습니다: {}", body);
                                        throw new IllegalStateException("Access Token을 받을 수 없습니다: " + body);
                                    }
                                    log.debug("한국투자증권 Access Token 발급 성공");
                                    return token;
                                });
                    } else {
                        // 에러 응답 본문 읽기
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("(응답 본문 없음)")
                                .flatMap(errorBody -> {
                                    // JSON 응답인 경우 파싱 시도
                                    String errorMessage = parseErrorMessage(errorBody);

                                    KoreaInvestmentApiLogging.logResponseError("접근토큰발급", response.statusCode().value(), errorBody);
                                    if (log.isDebugEnabled()) {
                                        log.debug("  [DEBUG] appKey(actual)={}", appKey);
                                    }

                                    // 403 Forbidden인 경우 인증 실패로 간주
                                    if (response.statusCode().value() == 403) {
                                        return Mono.error(new RuntimeException(
                                                String.format("한국투자증권 API 인증 실패 (403 Forbidden). " +
                                                        "App Key와 App Secret을 확인하세요. " +
                                                        "서버 타입(모의투자/실거래)이 API 키와 일치하는지 확인하세요. " +
                                                        "에러 상세: %s", errorMessage)));
                                    }

                                    return Mono.error(new RuntimeException(
                                            String.format("Access Token 발급 실패 (status: %s, message: %s)",
                                                    response.statusCode(), errorMessage)));
                                });
                    }
                })
                .timeout(Duration.ofSeconds(30))
                .onErrorMap(error -> {
                    if (error instanceof RuntimeException && error.getMessage().contains("Access Token 발급 실패")) {
                        return error;
                    }
                    log.error("한국투자증권 Access Token 발급 중 예외 발생: baseUrl={}, appKey={}",
                            baseUrl, LogMaskingUtil.maskApiKey(appKey), error);
                    if (log.isDebugEnabled()) {
                        log.debug("  [DEBUG] appKey(actual)={}", appKey);
                    }
                    return new RuntimeException("Access Token 발급 실패", error);
                });
    }

    /**
     * WebSocket 구독용 approval_key 발급.
     * POST /oauth2/Approval, Authorization: Bearer {accessToken}.
     * 응답에 approval_key가 있으면 반환, 없으면 빈 문자열.
     *
     * @param accessToken 접근토큰 (Bearer로 전달)
     * @param serverType  "1": 모의투자, "0": 실거래
     * @return approval_key 또는 빈 문자열
     */
    public Mono<String> getApprovalKey(String accessToken, String serverType) {
        if (accessToken == null || accessToken.isBlank()) {
            return Mono.just("");
        }
        String baseUrl = "1".equals(serverType != null ? serverType : "1") ? BASE_URL_VIRTUAL : BASE_URL_REAL;
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/oauth2/Approval")
                .build()
                .toUri();
        KoreaInvestmentApiLogging.logApiCallInfo("한국투자증권", "approval_key발급", uri.toString(), "POST");
        return webClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + accessToken)
                .bodyValue(Map.of())
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(Map.class)
                                .map(body -> {
                                    Object key = body != null ? body.get("approval_key") : null;
                                    return key != null ? key.toString() : "";
                                })
                                .defaultIfEmpty("");
                    }
                    return Mono.just("");
                })
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(e -> {
                    log.warn("approval_key 발급 실패(WebSocket 구독 시 설정에서 직접 입력 가능): {}", e.getMessage());
                    return Mono.just("");
                });
    }

    /**
     * 에러 응답 본문에서 메시지 추출
     * JSON 형식인 경우 파싱하여 메시지 추출, 그렇지 않으면 원본 반환
     */
    private String parseErrorMessage(String errorBody) {
        if (errorBody == null || errorBody.trim().isEmpty() || errorBody.equals("(응답 본문 없음)")) {
            return "에러 응답 본문이 없습니다";
        }

        try {
            // JSON 형식인 경우 파싱 시도
            @SuppressWarnings("unchecked")
            Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);

            // 일반적인 에러 메시지 필드 확인
            if (errorMap.containsKey("message")) {
                return String.valueOf(errorMap.get("message"));
            }
            if (errorMap.containsKey("msg")) {
                return String.valueOf(errorMap.get("msg"));
            }
            if (errorMap.containsKey("error")) {
                return String.valueOf(errorMap.get("error"));
            }
            if (errorMap.containsKey("error_description")) {
                return String.valueOf(errorMap.get("error_description"));
            }

            // 파싱은 성공했지만 메시지 필드가 없는 경우 전체 맵 반환
            return errorMap.toString();
        } catch (Exception e) {
            // JSON 파싱 실패 시 원본 반환
            return errorBody;
        }
    }
}
