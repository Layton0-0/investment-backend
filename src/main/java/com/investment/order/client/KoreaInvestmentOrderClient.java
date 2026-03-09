package com.investment.order.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.common.logging.KoreaInvestmentApiLogging;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserAccountRepository;
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
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한국투자증권 주문 API 클라이언트
 *
 * 한국투자증권 Open API를 사용하여 주문을 실행합니다.
 * - 국내주식: 주식주문(현금) /uapi/domestic-stock/v1/trading/order-cash
 * - 해외주식: 해외주식 주문 /uapi/overseas-stock/v1/trading/order (미국 NASD/NYSE/AMEX)
 *
 * TR ID (국내):
 * - 실거래: TTTC0012U (매수), TTTC0011U (매도)
 * - 모의투자: VTTC0012U (매수), VTTC0011U (매도)
 *
 * TR ID (해외 미국):
 * - 실거래: TTTT1002U (매수), TTTT1006U (매도)
 * - 모의투자: VTTT1002U (매수), VTTT1006U (매도)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KoreaInvestmentOrderClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final UserAccountRepository userAccountRepository;
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
     * local 환경에서 API 요청 상세 로그 출력 (토큰·appsecret 마스킹)
     */
    private void logApiRequest(String apiName, URI uri, HttpHeaders headers, Map<String, String> requestBody) {
        if (isLocalProfile()) {
            log.debug("=== {} API 요청 ===", apiName);
            log.debug("URL: {}", uri);
            if (headers != null) {
                String auth = headers.getFirst("Authorization");
                String appkey = headers.getFirst("appkey");
                String appsecret = headers.getFirst("appsecret");
                log.debug("Headers: Authorization={}, appkey={}, appsecret={}, tr_id={}, hashkey=(masked)",
                        auth != null ? "Bearer ***" : "null",
                        appkey != null ? LogMaskingUtil.maskApiKey(appkey) : "null",
                        appsecret != null ? LogMaskingUtil.maskSecret(appsecret) : "null",
                        headers.getFirst("tr_id"));
            }
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
     * 계좌번호에 해당하는 서버 타입 조회 (모의/실거래 구분)
     */
    private String resolveServerTypeForAccount(String userId, String accountNo) {
        if (userId == null || accountNo == null || accountNo.trim().isEmpty()) {
            return null;
        }
        List<UserAccount> accounts = userAccountRepository.findByUserIdAndBrokerType(userId,
                BrokerType.KOREA_INVESTMENT);
        for (UserAccount account : accounts) {
            try {
                String decrypted = encryptionUtil.decrypt(account.getAccountNoEncrypted());
                if (accountNo.trim().equals(decrypted)) {
                    return account.getServerType() != null ? account.getServerType() : "1";
                }
            } catch (Exception e) {
                log.trace("계좌번호 복호화 스킵: accountId={}", account.getId());
            }
        }
        return null;
    }

    /**
     * 계좌번호에 해당하는 API 키 조회 (모의/실거래 구분하여 올바른 키 반환).
     * 모의/실 혼용 방지: 계좌의 serverType과 일치하는 API 키만 사용. fallback으로 타 서버 키 사용 금지.
     */
    private Optional<UserApiKey> getUserApiKeyForAccount(String userId, String accountNo) {
        String serverType = resolveServerTypeForAccount(userId, accountNo);
        if (serverType == null) {
            log.warn("[주문] 계좌에 대한 serverType 미확인: accountNo={} (UserAccount에 동일 계좌 등록 및 serverType 설정 필요)", LogMaskingUtil.maskAccountNo(accountNo));
            return Optional.empty();
        }
        Optional<UserApiKey> byServer = userApiKeyRepository.findByUserIdAndBrokerTypeAndServerType(userId,
                BrokerType.KOREA_INVESTMENT, serverType);
        if (byServer.isEmpty()) {
            log.warn("[주문] 계좌 serverType({})과 일치하는 API 키 없음: accountNo={} (TB_USER_API_KEYS에 serverType={} 등록 필요)", serverType, LogMaskingUtil.maskAccountNo(accountNo), serverType);
        }
        return byServer;
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

        // 계좌별 API 키 조회 (모의/실거래 구분)
        UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                .orElseThrow(() -> new IllegalStateException(
                        "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId + ", accountNo=" + accountNo));

        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";

        // 계좌별 토큰 조회 (서버 타입별)
        String accessToken = tokenService.getAccessToken(userId, serverType);

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
        KoreaInvestmentApiLogging.logRequest("국내주식매수", "/uapi/domestic-stock/v1/trading/order-cash", trId, requestBody.keySet());

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
                        .exchangeToMono((ClientResponse response) -> {
                            int status = response.statusCode().value();
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .doOnNext(body -> log.warn("[국내주식매수] 한투 API 원시응답 status={} bodyPreview={}", status,
                                            (body != null && !body.isEmpty()) ? body.substring(0, Math.min(800, body.length())) : "(empty)"))
                                    .flatMap(body -> {
                                        if (status >= 400) {
                                            log.warn("[국내주식매수] 한투 API 실패 status={} body={}", status, body != null ? body : "(null)");
                                            return Mono.error(new RuntimeException("한투 주문 API " + status + ": " + (body != null ? body : "")));
                                        }
                                        if (body == null || body.trim().isEmpty()) {
                                            return Mono.error(new RuntimeException("한투 주문 API 200 but empty body"));
                                        }
                                        try {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                                            return Mono.just(map);
                                        } catch (Exception e) {
                                            log.warn("[국내주식매수] 한투 API 본문 파싱 실패 bodyPreview={}", body.length() > 200 ? body.substring(0, 200) : body, e);
                                            return Mono.error(new RuntimeException("한투 주문 API 본문 파싱 실패: " + e.getMessage(), e));
                                        }
                                    });
                        })
                        .timeout(Duration.ofSeconds(10))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(throwable -> {
                                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                        return ex.getStatusCode().is5xxServerError();
                                    }
                                    return false;
                                }))
                        .map(responseMap -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMapTyped = (Map<String, Object>) responseMap;

                            String rtCd = (String) responseMapTyped.get("rt_cd");
                            String msgCd = (String) responseMapTyped.get("msg_cd");
                            String msg1 = (String) responseMapTyped.get("msg1");
                            if (rtCd == null || !"0".equals(rtCd)) {
                                KoreaInvestmentApiLogging.logResponseError("국내주식매수", 200, rtCd, msgCd, msg1, null);
                                throw new RuntimeException(
                                        "한국투자증권 주문 API 오류: rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
                            }

                            KoreaInvestmentApiLogging.logResponseSuccessFromMap("국내주식매수", 200, responseMapTyped);

                            @SuppressWarnings("unchecked")
                            Map<String, Object> output = (Map<String, Object>) responseMapTyped.get("output");
                            if (output == null) {
                                KoreaInvestmentApiLogging.logResponseError("국내주식매수", 200, rtCd, msgCd, "output 없음", null);
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
                            KoreaInvestmentApiLogging.logFailure("국내주식매수", throwable);
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

        // 계좌별 API 키 조회 (모의/실거래 구분)
        UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                .orElseThrow(() -> new IllegalStateException(
                        "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId + ", accountNo=" + accountNo));

        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";

        // 계좌별 토큰 조회 (서버 타입별)
        String accessToken = tokenService.getAccessToken(userId, serverType);

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
        KoreaInvestmentApiLogging.logRequest("국내주식매도", "/uapi/domestic-stock/v1/trading/order-cash", trId, requestBody.keySet());

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

                            String rtCd = (String) responseMap.get("rt_cd");
                            String msgCd = (String) responseMap.get("msg_cd");
                            String msg1 = (String) responseMap.get("msg1");
                            if (rtCd == null || !"0".equals(rtCd)) {
                                KoreaInvestmentApiLogging.logResponseError("국내주식매도", 200, rtCd, msgCd, msg1, null);
                                throw new RuntimeException(
                                        "한국투자증권 주문 API 오류: rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
                            }

                            KoreaInvestmentApiLogging.logResponseSuccessFromMap("국내주식매도", 200, responseMap);

                            @SuppressWarnings("unchecked")
                            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
                            if (output == null) {
                                KoreaInvestmentApiLogging.logResponseError("국내주식매도", 200, rtCd, msgCd, "output 없음", null);
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
                            KoreaInvestmentApiLogging.logFailure("국내주식매도", throwable);
                            return new RuntimeException("주문 실행 실패: " + throwable.getMessage(), throwable);
                        }));
    }

    /** 해외(미국) 매수 TR_ID 실거래 */
    private static final String OVERSEAS_BUY_TR_ID = "TTTT1002U";
    /** 해외(미국) 매도 TR_ID 실거래 */
    private static final String OVERSEAS_SELL_TR_ID = "TTTT1006U";
    /** 미국 거래소 코드 (나스닥) */
    private static final String OVRS_EXCG_CD_US = "NASD";

    /**
     * 해외주식 매수 주문 (미국: NASD/NYSE/AMEX).
     * MCP 스펙: /uapi/overseas-stock/v1/trading/order, TR_ID
     * TTTT1002U(실거래)/VTTT1002U(모의).
     *
     * @param userId    사용자 ID
     * @param accountNo 계좌번호 (8-2 체계)
     * @param symbol    종목 티커 (예: AAPL, TSLA)
     * @param quantity  주문 수량
     * @param price     주문 단가 (지정가). 시장가 시 "0" 전달
     * @param orderDvsn 주문구분 ("00": 지정가, 모의투자는 00만 가능)
     * @return 주문 응답 (주문번호 포함)
     */
    @SuppressWarnings("unchecked")
    public Mono<OrderResponse> placeOverseasBuyOrder(String userId, String accountNo, String symbol,
            Integer quantity, BigDecimal price, String orderDvsn) {
        log.info("해외주식 매수 주문: userId={}, accountNo={}, symbol={}, quantity={}, price={}",
                LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), symbol, quantity, price);

        UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                .orElseThrow(() -> new IllegalStateException(
                        "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId + ", accountNo=" + accountNo));
        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";
        String accessToken = tokenService.getAccessToken(userId, serverType);
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

        String baseUrl = getBaseUrl(serverType);
        String trId = getTrId(OVERSEAS_BUY_TR_ID, serverType);
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/uapi/overseas-stock/v1/trading/order")
                .build()
                .toUri();

        String ovrsOrdUnpr = (price != null && price.compareTo(BigDecimal.ZERO) > 0)
                ? price.toPlainString()
                : "0";
        Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                accountNo,
                Map.of(
                        "OVRS_EXCG_CD", OVRS_EXCG_CD_US,
                        "PDNO", symbol,
                        "ORD_QTY", String.valueOf(quantity),
                        "OVRS_ORD_UNPR", ovrsOrdUnpr,
                        "CTAC_TLNO", "",
                        "MGCO_APTM_ODNO", "",
                        "SLL_TYPE", "",
                        "ORD_SVR_DVSN_CD", "0",
                        "ORD_DVSN", orderDvsn != null ? orderDvsn : "00"));

        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBodyForHash = (Map<String, Object>) (Map<?, ?>) requestBody;
        String hashkey = hashkeyUtil.generateHashkey(requestBodyForHash, appSecret);
        headers.set("hashkey", hashkey);

        logApiRequest("해외주식 매수 주문", uri, headers, requestBody);
        KoreaInvestmentApiLogging.logRequest("해외주식매수", "/uapi/overseas-stock/v1/trading/order", trId, requestBody.keySet());
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
                        .map(response -> parseOverseasOrderResponse((Map<String, Object>) response, symbol, quantity,
                                price, "BUY", "해외주식매수"))
                        .onErrorMap(throwable -> {
                            KoreaInvestmentApiLogging.logFailure("해외주식매수", throwable);
                            return new RuntimeException("주문 실행 실패: " + throwable.getMessage(), throwable);
                        }));
    }

    /**
     * 해외주식 매도 주문 (미국).
     * MCP 스펙: /uapi/overseas-stock/v1/trading/order, TR_ID
     * TTTT1006U(실거래)/VTTT1006U(모의).
     */
    @SuppressWarnings("unchecked")
    public Mono<OrderResponse> placeOverseasSellOrder(String userId, String accountNo, String symbol,
            Integer quantity, BigDecimal price, String orderDvsn) {
        log.info("해외주식 매도 주문: userId={}, accountNo={}, symbol={}, quantity={}, price={}",
                LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), symbol, quantity, price);

        UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                .orElseThrow(() -> new IllegalStateException(
                        "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId + ", accountNo=" + accountNo));
        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";
        String accessToken = tokenService.getAccessToken(userId, serverType);
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

        String baseUrl = getBaseUrl(serverType);
        String trId = getTrId(OVERSEAS_SELL_TR_ID, serverType);
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/uapi/overseas-stock/v1/trading/order")
                .build()
                .toUri();

        String ovrsOrdUnpr = (price != null && price.compareTo(BigDecimal.ZERO) > 0)
                ? price.toPlainString()
                : "0";
        Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                accountNo,
                Map.of(
                        "OVRS_EXCG_CD", OVRS_EXCG_CD_US,
                        "PDNO", symbol,
                        "ORD_QTY", String.valueOf(quantity),
                        "OVRS_ORD_UNPR", ovrsOrdUnpr,
                        "CTAC_TLNO", "",
                        "MGCO_APTM_ODNO", "",
                        "SLL_TYPE", "00",
                        "ORD_SVR_DVSN_CD", "0",
                        "ORD_DVSN", orderDvsn != null ? orderDvsn : "00"));

        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);
        @SuppressWarnings("unchecked")
        Map<String, Object> requestBodyForHash = (Map<String, Object>) (Map<?, ?>) requestBody;
        String hashkey = hashkeyUtil.generateHashkey(requestBodyForHash, appSecret);
        headers.set("hashkey", hashkey);

        logApiRequest("해외주식 매도 주문", uri, headers, requestBody);
        KoreaInvestmentApiLogging.logRequest("해외주식매도", "/uapi/overseas-stock/v1/trading/order", trId, requestBody.keySet());
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
                        .map(response -> parseOverseasOrderResponse((Map<String, Object>) response, symbol, quantity,
                                price, "SELL", "해외주식매도"))
                        .onErrorMap(throwable -> {
                            KoreaInvestmentApiLogging.logFailure("해외주식매도", throwable);
                            return new RuntimeException("주문 실행 실패: " + throwable.getMessage(), throwable);
                        }));
    }

    @SuppressWarnings("unchecked")
    private OrderResponse parseOverseasOrderResponse(Map<String, Object> responseMap, String symbol,
            Integer quantity, BigDecimal price, String orderType, String apiName) {
        String rtCd = (String) responseMap.get("rt_cd");
        String msgCd = (String) responseMap.get("msg_cd");
        String msg1 = (String) responseMap.get("msg1");
        if (rtCd == null || !"0".equals(rtCd)) {
            KoreaInvestmentApiLogging.logResponseError(apiName, 200, rtCd, msgCd, msg1, null);
            throw new RuntimeException(
                    "한국투자증권 해외주문 API 오류: rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
        }
        KoreaInvestmentApiLogging.logResponseSuccessFromMap(apiName, 200, responseMap);
        Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
        if (output == null) {
            KoreaInvestmentApiLogging.logResponseError(apiName, 200, rtCd, msgCd, "output 없음", null);
            throw new RuntimeException("한국투자증권 해외주문 API 응답에 output이 없습니다");
        }
        String orderNo = (String) output.get("ODNO");
        if (orderNo == null) {
            orderNo = (String) output.get("odno");
        }
        if (orderNo == null && !output.isEmpty()) {
            orderNo = String.valueOf(output.getOrDefault("ORD_NO", output.values().iterator().next()));
        }
        return OrderResponse.builder()
                .orderNo(orderNo != null ? orderNo : "")
                .symbol(symbol)
                .quantity(quantity)
                .price(price)
                .orderType(orderType)
                .status("SUCCESS")
                .build();
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
