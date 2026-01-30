package com.investment.order.client;

import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.KoreaInvestmentHashkeyUtil;
import com.investment.marketdata.util.KoreaInvestmentRequestBuilder;
import com.investment.marketdata.util.StockCodeConverter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * 한국투자증권 주문 API 클라이언트
 * 
 * 한국투자증권 Open API를 사용하여 주문을 실행합니다.
 * - 주식 매수 주문
 * - 주식 매도 주문
 * - 주문 취소
 * 
 * TR ID:
 * - 실거래: TTTC0012U (매수), TTTC0011U (매도)
 * - 모의투자: VTTC0012U (매수), VTTC0011U (매도)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KoreaInvestmentOrderClient {

    private final WebClient webClient;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final EncryptionUtil encryptionUtil;
    private final Environment environment;
    private final KoreaInvestmentHashkeyUtil hashkeyUtil;

    // Rate Limiter 인스턴스 이름
    private static final String RATE_LIMITER_API_VIRTUAL = "koreaInvestmentApi";
    private static final String RATE_LIMITER_API_REAL = "koreaInvestmentApiReal";

    // 한국투자증권 API Base URL
    private static final String BASE_URL_REAL = "https://openapi.koreainvestment.com:9443"; // 실거래
    private static final String BASE_URL_VIRTUAL = "https://openapivts.koreainvestment.com:29443"; // 모의투자

    /**
     * local 환경 여부 확인
     */
    private boolean isLocalProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("local".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * local 환경에서 API 요청 상세 로그 출력
     */
    private void logApiRequest(String apiName, URI uri, HttpHeaders headers, Map<String, String> requestBody) {
        if (isLocalProfile()) {
            log.debug("=== {} API 요청 ===", apiName);
            log.debug("URL: {}", uri);
            log.debug("Headers: {}", headers);
            log.debug("RequestBody: {}", requestBody);
        }
    }

    /**
     * Base URL 가져오기
     */
    private String getBaseUrl(String serverType) {
        return "0".equals(serverType) ? BASE_URL_REAL : BASE_URL_VIRTUAL;
    }

    /**
     * TR ID 가져오기 (실거래/모의투자 구분)
     */
    private String getTrId(String baseTrId, String serverType) {
        if ("0".equals(serverType)) {
            // 실거래
            return baseTrId;
        } else {
            // 모의투자: 앞에 'V' 추가
            return "V" + baseTrId.substring(1);
        }
    }

    /**
     * Rate Limiter 가져오기
     */
    private RateLimiter getApiRateLimiter(String serverType) {
        if ("0".equals(serverType)) {
            // 실전투자: 1초당 20건
            return rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_REAL);
        } else {
            // 모의투자: 1초당 2건
            return rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_VIRTUAL);
        }
    }

    /**
     * 주식 매수 주문
     * 
     * @param userId    사용자 ID
     * @param accountNo 계좌번호 (8자리)
     * @param symbol    종목 코드
     * @param quantity  주문 수량
     * @param price     주문 가격 (시장가인 경우 null)
     * @param orderType 주문 유형 ("00": 지정가, "01": 시장가)
     * @return 주문 응답 (주문번호 포함)
     */
    public Mono<OrderResponse> placeBuyOrder(String userId, String accountNo, String symbol,
            Integer quantity, BigDecimal price, String orderType) {
        log.info("주식 매수 주문: userId={}, accountNo={}, symbol={}, quantity={}, price={}, orderType={}",
                LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), symbol, quantity, price,
                orderType);

        // 종목 코드 변환 (6자리)
        String stockCode = StockCodeConverter.toStockCode(symbol);

        // 사용자 API 키 조회
        UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                .orElseThrow(() -> new IllegalStateException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

        // 토큰 조회
        String accessToken = tokenService.getAccessToken(userId);
        String serverType = userApiKey.getServerType();

        // API 키 복호화
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

        String baseUrl = getBaseUrl(serverType);
        String trId = getTrId("TTTC0012U", serverType); // 주식현재가 매수 주문 TR ID

        // API 엔드포인트
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/uapi/domestic-stock/v1/trading/order-cash")
                .build()
                .toUri();

        // 요청 바디 생성
        Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                accountNo,
                Map.of(
                        "PDNO", stockCode, // 종목코드
                        "ORD_DVSN", orderType != null ? orderType : "00", // 주문구분 (00: 지정가, 01: 시장가)
                        "ORD_QTY", String.valueOf(quantity), // 주문수량
                        "ORD_UNPR", price != null ? price.toPlainString() : "0", // 주문단가 (시장가인 경우 0)
                        "EXCG_ID_DVSN_CD", "KRX" // 거래소ID구분코드 (KRX: 한국거래소)
                ));

        // 공통 헤더 생성
        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);

        // Hashkey 생성 (주문 API는 Hashkey 필수)
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBodyForHash = (Map<String, Object>) (Map<?, ?>) requestBody;
        String hashkey = hashkeyUtil.generateHashkey(requestBodyForHash, appSecret);
        headers.set("hashkey", hashkey);

        logApiRequest("주식 매수 주문", uri, headers, requestBody);

        // Rate Limiter 적용
        RateLimiter rateLimiter = getApiRateLimiter(serverType);

        return Mono.fromCallable(() -> {
            rateLimiter.acquirePermission();
            return null;
        })
                .flatMap(ignored -> webClient.post()
                        .uri(uri)
                        .headers(h -> h.addAll(headers))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(10))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(throwable -> {
                                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                        return ex.getStatusCode().is5xxServerError();
                                    }
                                    return false;
                                }))
                        .map(response -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMap = (Map<String, Object>) response;

                            // rt_cd 체크
                            String rtCd = (String) responseMap.get("rt_cd");
                            if (rtCd == null || !"0".equals(rtCd)) {
                                String msg1 = (String) responseMap.get("msg1");
                                String msgCd = (String) responseMap.get("msg_cd");
                                throw new RuntimeException(
                                        "한국투자증권 주문 API 오류: rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
                            }

                            // output 파싱
                            @SuppressWarnings("unchecked")
                            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
                            if (output == null) {
                                throw new RuntimeException("한국투자증권 API 응답에 output이 없습니다");
                            }

                            String orderNo = (String) output.get("ODNO"); // 주문번호

                            return OrderResponse.builder()
                                    .orderNo(orderNo)
                                    .symbol(stockCode)
                                    .quantity(quantity)
                                    .price(price)
                                    .orderType("BUY")
                                    .status("SUCCESS")
                                    .build();
                        })
                        .onErrorMap(throwable -> {
                            log.error("주식 매수 주문 실패: userId={}, accountNo={}, symbol={}",
                                    userId, accountNo, symbol, throwable);
                            return new RuntimeException("주문 실행 실패: " + throwable.getMessage(), throwable);
                        }));
    }

    /**
     * 주식 매도 주문
     * 
     * @param userId    사용자 ID
     * @param accountNo 계좌번호 (8자리)
     * @param symbol    종목 코드
     * @param quantity  주문 수량
     * @param price     주문 가격 (시장가인 경우 null)
     * @param orderType 주문 유형 ("00": 지정가, "01": 시장가)
     * @return 주문 응답 (주문번호 포함)
     */
    public Mono<OrderResponse> placeSellOrder(String userId, String accountNo, String symbol,
            Integer quantity, BigDecimal price, String orderType) {
        log.info("주식 매도 주문: userId={}, accountNo={}, symbol={}, quantity={}, price={}, orderType={}",
                LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), symbol, quantity, price,
                orderType);

        // 종목 코드 변환 (6자리)
        String stockCode = StockCodeConverter.toStockCode(symbol);

        // 사용자 API 키 조회
        UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                .orElseThrow(() -> new IllegalStateException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

        // 토큰 조회
        String accessToken = tokenService.getAccessToken(userId);
        String serverType = userApiKey.getServerType();

        // API 키 복호화
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

        String baseUrl = getBaseUrl(serverType);
        String trId = getTrId("TTTC0011U", serverType); // 주식현재가 매도 주문 TR ID

        // API 엔드포인트
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/uapi/domestic-stock/v1/trading/order-cash")
                .build()
                .toUri();

        // 요청 바디 생성
        Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                accountNo,
                Map.of(
                        "PDNO", stockCode, // 종목코드
                        "ORD_DVSN", orderType != null ? orderType : "00", // 주문구분 (00: 지정가, 01: 시장가)
                        "ORD_QTY", String.valueOf(quantity), // 주문수량
                        "ORD_UNPR", price != null ? price.toPlainString() : "0", // 주문단가 (시장가인 경우 0)
                        "EXCG_ID_DVSN_CD", "KRX" // 거래소ID구분코드 (KRX: 한국거래소)
                ));

        // 공통 헤더 생성
        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);

        // Hashkey 생성 (주문 API는 Hashkey 필수)
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBodyForHash = (Map<String, Object>) (Map<?, ?>) requestBody;
        String hashkey = hashkeyUtil.generateHashkey(requestBodyForHash, appSecret);
        headers.set("hashkey", hashkey);

        logApiRequest("주식 매도 주문", uri, headers, requestBody);

        // Rate Limiter 적용
        RateLimiter rateLimiter = getApiRateLimiter(serverType);

        return Mono.fromCallable(() -> {
            rateLimiter.acquirePermission();
            return null;
        })
                .flatMap(ignored -> webClient.post()
                        .uri(uri)
                        .headers(h -> h.addAll(headers))
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(10))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(throwable -> {
                                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                        return ex.getStatusCode().is5xxServerError();
                                    }
                                    return false;
                                }))
                        .map(response -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMap = (Map<String, Object>) response;

                            // rt_cd 체크
                            String rtCd = (String) responseMap.get("rt_cd");
                            if (rtCd == null || !"0".equals(rtCd)) {
                                String msg1 = (String) responseMap.get("msg1");
                                String msgCd = (String) responseMap.get("msg_cd");
                                throw new RuntimeException(
                                        "한국투자증권 주문 API 오류: rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
                            }

                            // output 파싱
                            @SuppressWarnings("unchecked")
                            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
                            if (output == null) {
                                throw new RuntimeException("한국투자증권 API 응답에 output이 없습니다");
                            }

                            String orderNo = (String) output.get("ODNO"); // 주문번호

                            return OrderResponse.builder()
                                    .orderNo(orderNo)
                                    .symbol(stockCode)
                                    .quantity(quantity)
                                    .price(price)
                                    .orderType("SELL")
                                    .status("SUCCESS")
                                    .build();
                        })
                        .onErrorMap(throwable -> {
                            log.error("주식 매도 주문 실패: userId={}, accountNo={}, symbol={}",
                                    LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), symbol,
                                    throwable);
                            return new RuntimeException("주문 실행 실패: " + throwable.getMessage(), throwable);
                        }));
    }

    /**
     * 주문 응답 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class OrderResponse {
        private String orderNo; // 주문번호
        private String symbol; // 종목코드
        private Integer quantity; // 주문수량
        private BigDecimal price; // 주문가격
        private String orderType; // 주문유형 (BUY, SELL)
        private String status; // 주문상태 (SUCCESS, FAILED)
    }
}
