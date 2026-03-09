package com.investment.marketdata.client.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.client.IndicatorResponse;
import com.investment.marketdata.client.MarketDataClient;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.KoreaInvestmentHashkeyUtil;
import com.investment.common.logging.KoreaInvestmentApiLogging;
import com.investment.marketdata.util.KoreaInvestmentRequestBuilder;
import com.investment.marketdata.util.StockCodeConverter;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국투자증권 API 시장 데이터 클라이언트 구현체
 * 
 * 한국투자증권 Open API는 REST API 기반으로 제공됩니다.
 * - OAuth 2.0 인증 (App Key, App Secret)
 * - Access Token 발급 후 API 호출
 * - 종목 코드: 6자리 숫자 (코스피/코스닥)
 * 
 * API 문서: https://apiportal.koreainvestment.com/
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "investment.market-data.provider", havingValue = "korea-investment")
public class KoreaInvestmentMarketDataClient implements MarketDataClient {

    private final MarketDataProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final EncryptionUtil encryptionUtil;
    private final KoreaInvestmentHashkeyUtil hashkeyUtil;

    // 한국투자증권 API Base URL
    private static final String BASE_URL_REAL = "https://openapi.koreainvestment.com:9443"; // 실거래
    private static final String BASE_URL_VIRTUAL = "https://openapivts.koreainvestment.com:29443"; // 모의투자

    // Rate Limiter 인스턴스 이름
    private static final String RATE_LIMITER_API_VIRTUAL = "koreaInvestmentApi";
    private static final String RATE_LIMITER_API_REAL = "koreaInvestmentApiReal";

    /** 요청 내 토큰/API키 재사용: userId → (tokenInfo, 만료시각). TTL 5초로 동일 요청 내 N+1 제거. */
    private static final long TOKEN_INFO_CACHE_TTL_MS = 5_000;
    private final Map<String, CachedTokenInfo> tokenInfoCache = new ConcurrentHashMap<>();

    private static final class CachedTokenInfo {
        final Map<String, String> info;
        final long expireAt;

        CachedTokenInfo(Map<String, String> info, long expireAt) {
            this.info = info;
            this.expireAt = expireAt;
        }
    }

    /**
     * 현재 사용자 ID 가져오기
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다");
        }
        return authentication.getName();
    }

    /**
     * API 호출용 Rate Limiter 가져오기
     * 서버 타입(실전투자/모의투자)에 따라 적절한 Rate Limiter 반환
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
     * 조회 API용 URI 생성 (GET + query parameter).
     * 한국투자증권 시세 조회 API는 GET 메서드에 query parameter로 전달한다.
     */
    private URI buildUriWithQueryParams(String baseUrl, String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        return builder.build().toUri();
    }

    @Override
    public Mono<IndicatorResponse> getIndicator(String indicator, String symbol, String interval) {
        log.debug("한국투자증권 API 호출: indicator={}, symbol={}, interval={}", indicator, symbol, interval);

        // 현재 사용자 ID 가져오기
        String userId = getCurrentUserId();

        // Access Token 확인 및 갱신 (동일 요청 내 캐시로 N+1 제거)
        return ensureAccessToken(userId)
                .flatMap(tokenInfo -> {
                    // 종목 코드 변환 (6자리 종목코드)
                    String stockCode = StockCodeConverter.toStockCode(symbol);

                    // 차트 데이터 조회 (한국투자증권 API는 차트 데이터를 제공하고, 클라이언트에서 지표 계산)
                    return getChartData(stockCode, interval, tokenInfo)
                            .map(chartData -> calculateIndicator(chartData, indicator))
                            .onErrorResume(error -> {
                                log.error("한국투자증권 API 호출 실패: indicator={}, symbol={}", indicator, symbol, error);
                                return Mono.just(createErrorResponse("API 호출 실패: " + error.getMessage()));
                            });
                })
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .onErrorReturn(createErrorResponse("타임아웃 또는 오류 발생"));
    }

    @Override
    public Mono<Map<String, IndicatorResponse>> getBulkIndicators(
            String symbol, String interval, String... indicators) {

        log.debug("한국투자증권 Bulk API 호출: symbol={}, interval={}, indicators={}", symbol, interval, indicators);

        // 종목 코드 변환
        String stockCode = StockCodeConverter.toStockCode(symbol);

        // 현재 사용자 ID 가져오기
        String userId = getCurrentUserId();

        // Access Token 확인 및 갱신 (동일 요청 내 캐시로 N+1 제거)
        return ensureAccessToken(userId)
                .flatMap(tokenInfo -> {
                    // 차트 데이터를 한 번만 조회
                    return getChartData(stockCode, interval, tokenInfo)
                            .map(chartData -> {
                                // 모든 지표를 계산
                                Map<String, IndicatorResponse> resultMap = new HashMap<>();
                                for (String indicator : indicators) {
                                    IndicatorResponse response = calculateIndicator(chartData, indicator);
                                    resultMap.put(indicator, response);
                                }
                                return resultMap;
                            })
                            .onErrorResume(error -> {
                                log.error("한국투자증권 Bulk API 호출 실패: symbol={}", symbol, error);
                                // 실패한 지표는 에러 응답으로 채움
                                Map<String, IndicatorResponse> errorMap = new HashMap<>();
                                for (String indicator : indicators) {
                                    errorMap.put(indicator, createErrorResponse("API 호출 실패"));
                                }
                                return Mono.just(errorMap);
                            });
                })
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .onErrorReturn(new HashMap<>());
    }

    @Override
    public String getProviderName() {
        return "Korea Investment";
    }

    /**
     * 주식 현재가 조회
     * 
     * 한국투자증권 API의 주식현재가 조회 API를 사용합니다.
     * TR ID: 실전 FHPST01010100 / 모의 FHKST01010100
     * 
     * @param symbol 종목 코드 (6자리 또는 종목명)
     * @return 현재가 정보
     */
    public Mono<com.investment.marketdata.dto.CurrentPriceDto> getCurrentPrice(String symbol) {
        String userId;
        try {
            userId = getCurrentUserId();
        } catch (Exception e) {
            log.warn("[주식현재가] 사용자 인증 없음: symbol={}, error={}", symbol, e.getMessage());
            return Mono.error(e);
        }
        String stockCode = StockCodeConverter.toStockCode(symbol);
        log.info("[주식현재가] 요청 시작 symbol={} stockCode={} userId={}", symbol, stockCode, userId != null ? LogMaskingUtil.maskUserId(userId) : "null");

        // Access Token 확인 및 갱신 (동일 요청 내 캐시로 N+1 제거)
        return ensureAccessToken(userId)
                .flatMap(tokenInfo -> getCurrentPriceFromApi(stockCode, tokenInfo)
                        .onErrorResume(error -> {
                            log.warn("[주식현재가] 조회 실패 symbol={} stockCode={} error={}", symbol, stockCode, error.getMessage());
                            log.debug("[주식현재가] 조회 실패 상세", error);
                            return Mono.error(new RuntimeException("현재가 조회 실패: " + error.getMessage(), error));
                        }))
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .onErrorMap(throwable -> new RuntimeException("현재가 조회 타임아웃 또는 오류 발생", throwable));
    }

    /**
     * 한국투자증권 API를 통한 현재가 조회
     * tokenInfo: token, serverType, appKey, appSecret, userId (ensureAccessToken 결과, N+1 방지)
     */
    private Mono<com.investment.marketdata.dto.CurrentPriceDto> getCurrentPriceFromApi(
            String stockCode, Map<String, String> tokenInfo) {
        String accessToken = tokenInfo.get("token");
        String serverType = tokenInfo.get("serverType");
        String appKey = tokenInfo.get("appKey");
        String appSecret = tokenInfo.get("appSecret");
        String userId = tokenInfo.get("userId");

        String baseUrl = getBaseUrl(serverType);
        // 주식현재가 시세(v1_국내주식-008): 공식 샘플(open-trading-api inquire_price)은 실전/모의 모두 FHKST01010100 사용. FHPST 사용 시 404 발생 가능.
        String trId = getCurrentPriceTrId(serverType);

        // 조회 파라미터 (GET query). Postman과 동일 순서: FID_COND_MRKT_DIV_CODE → FID_INPUT_ISCD
        Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createMarketDataRequestBody(
                new java.util.LinkedHashMap<>(java.util.Map.of(
                        "FID_COND_MRKT_DIV_CODE", "J", // J: 주식, ETF, ETN
                        "FID_INPUT_ISCD", stockCode    // 종목코드 6자리 (예: 005930)
                )));
        URI uri = buildUriWithQueryParams(baseUrl, "/uapi/domestic-stock/v1/quotations/inquire-price", queryParams);
        log.info("[주식현재가] 한투 API 요청 fullUrl={} queryParams={} trId={} serverType={}", uri.toString(), queryParams, trId, serverType);

        // 요청 헤더: Postman과 동일하게 Authorization, appkey, appsecret, tr_id, Content-Type, custtype
        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);
        headers.set("custtype", "P"); // 고객유형: P(개인)
        if (log.isDebugEnabled()) {
            log.debug("[주식현재가] 전송 헤더: Authorization(Bearer), appkey, appsecret, tr_id={}, Content-Type=application/json, custtype=P", trId);
        }

        String path = "/uapi/domestic-stock/v1/quotations/inquire-price";
        KoreaInvestmentApiLogging.logRequest("주식현재가", path, trId, queryParams);

        // Rate Limiter 적용
        RateLimiter rateLimiter = getApiRateLimiter(serverType);

        return Mono.fromCallable(() -> {
            rateLimiter.acquirePermission();
            return null;
        })
                .flatMap(ignored -> webClient.get()
                        .uri(uri)
                        .headers(h -> h.addAll(headers))
                        .exchangeToMono((ClientResponse response) -> {
                            int status = response.statusCode().value();
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .doOnNext(body -> log.warn("[주식현재가] 한투 API 원시응답 status={} bodyPreview={}", status,
                                            (body != null && !body.isEmpty()) ? body.substring(0, Math.min(1000, body.length())) : "(empty)"))
                                    .flatMap(body -> {
                                        if (status >= 400) {
                                            log.warn("[주식현재가] 한투 API 실패 status={} body={}", status, body != null ? body : "(null)");
                                            return Mono.error(new RuntimeException("한투 API " + status + ": " + (body != null ? body : "")));
                                        }
                                        if (body == null || body.trim().isEmpty()) {
                                            return Mono.error(new RuntimeException("한투 API 200 but empty body"));
                                        }
                                        try {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> map = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                                            return Mono.just(map);
                                        } catch (Exception e) {
                                            log.warn("[주식현재가] 한투 API 본문 파싱 실패 bodyPreview={}", body.length() > 200 ? body.substring(0, 200) : body, e);
                                            return Mono.error(new RuntimeException("한투 API 본문 파싱 실패: " + e.getMessage(), e));
                                        }
                                    });
                        })
                        .timeout(Duration.ofMillis(properties.getTimeout()))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(throwable -> {
                                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                        if (ex.getStatusCode().value() == 401) {
                                            log.warn("한국투자증권 현재가 401, 토큰 캐시 무효화 후 재시도: userId={}", LogMaskingUtil.maskUserId(userId));
                                            invalidateTokenCache(userId);
                                            return true;
                                        }
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
                                log.warn("[주식현재가] 한투 API 비정상 rt_cd={} msg_cd={} msg1={} stockCode={}", rtCd, msgCd, msg1, stockCode);
                                KoreaInvestmentApiLogging.logResponseError("주식현재가", 200, rtCd, msgCd, msg1, null);
                                throw new RuntimeException(
                                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg_cd=" + msgCd + ", msg1=" + msg1);
                            }

                            KoreaInvestmentApiLogging.logResponseSuccessFromMap("주식현재가", 200, responseMap);

                            // output 파싱
                            @SuppressWarnings("unchecked")
                            Map<String, Object> output = (Map<String, Object>) responseMap.get("output");
                            if (output == null) {
                                log.warn("[주식현재가] 응답 output 없음 stockCode={} responseKeys={}", stockCode, responseMap.keySet());
                                KoreaInvestmentApiLogging.logResponseError("주식현재가", 200, rtCd, null, "output 없음", null);
                                throw new RuntimeException("한국투자증권 API 응답에 output이 없습니다");
                            }
                            String stckPrpr = (String) output.getOrDefault("stck_prpr", "");
                            String htsKorIsnm = (String) output.getOrDefault("hts_kor_isnm", "");
                            log.info("[주식현재가] 한투 API 응답 성공 stockCode={} stck_prpr={} hts_kor_isnm={}", stockCode, stckPrpr, htsKorIsnm);

                            return parseCurrentPriceResponse(output, stockCode);
                        })
                        .doOnSuccess(v -> log.info("[주식현재가] Mono 완료 stockCode={} hasValue={} (false면 빈 완료→null 반환)", stockCode, v != null))
                        .doOnError(e -> {
                            log.warn("[주식현재가] Mono 에러 stockCode={} exception={} message={}", stockCode, e.getClass().getSimpleName(), e.getMessage());
                            if (!(e instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex)) {
                                KoreaInvestmentApiLogging.logFailure("주식현재가", e);
                            } else {
                                KoreaInvestmentApiLogging.logResponseError("주식현재가", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                            }
                        }));
    }

    /**
     * 현재가 응답 파싱
     */
    private com.investment.marketdata.dto.CurrentPriceDto parseCurrentPriceResponse(
            Map<String, Object> output, String stockCode) {

        // 한국투자증권 API 응답 필드명 매핑
        // 실제 API 응답 필드명에 맞게 수정 필요
        String name = (String) output.getOrDefault("hts_kor_isnm", "");
        String currentPriceStr = (String) output.getOrDefault("stck_prpr", "0");
        String changeRateStr = (String) output.getOrDefault("prdy_ctrt", "0");
        String changeAmountStr = (String) output.getOrDefault("prdy_vrss", "0");
        String previousCloseStr = (String) output.getOrDefault("stck_prdy_clpr", "0");
        String openPriceStr = (String) output.getOrDefault("stck_oprc", "0");
        String highPriceStr = (String) output.getOrDefault("stck_hgpr", "0");
        String lowPriceStr = (String) output.getOrDefault("stck_lwpr", "0");
        String volumeStr = (String) output.getOrDefault("acml_vol", "0");
        String tradingValueStr = (String) output.getOrDefault("acml_tr_pbmn", "0");
        String marketCapStr = (String) output.getOrDefault("hts_avls", "0");
        String listedSharesStr = (String) output.getOrDefault("lstc_stck_cnt", "0");

        return com.investment.marketdata.dto.CurrentPriceDto.builder()
                .symbol(stockCode)
                .name(name)
                .currentPrice(parseBigDecimal(currentPriceStr))
                .changeRate(parseBigDecimal(changeRateStr))
                .changeAmount(parseBigDecimal(changeAmountStr))
                .previousClose(parseBigDecimal(previousCloseStr))
                .openPrice(parseBigDecimal(openPriceStr))
                .highPrice(parseBigDecimal(highPriceStr))
                .lowPrice(parseBigDecimal(lowPriceStr))
                .volume(parseLong(volumeStr))
                .tradingValue(parseBigDecimal(tradingValueStr))
                .marketCap(parseBigDecimal(marketCapStr))
                .listedShares(parseLong(listedSharesStr))
                .queriedAt(java.time.LocalDateTime.now())
                .build();
    }

    /**
     * BigDecimal 파싱 헬퍼
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            // 한국투자증권 API는 숫자에 콤마가 포함될 수 있음
            String cleaned = value.replace(",", "").trim();
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            log.warn("BigDecimal 파싱 실패: value={}", value, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Long 파싱 헬퍼
     */
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            String cleaned = value.replace(",", "").trim();
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            log.warn("Long 파싱 실패: value={}", value, e);
            return 0L;
        }
    }

    /**
     * Access Token 확인 및 갱신.
     * 동일 요청 내(5초 TTL 캐시) 재호출 시 DB/발급 없이 캐시된 tokenInfo 재사용하여 N+1 제거.
     * tokenInfo: token, serverType, appKey, appSecret, userId.
     */
    private Mono<Map<String, String>> ensureAccessToken(String userId) {
        long now = System.currentTimeMillis();
        CachedTokenInfo cached = tokenInfoCache.get(userId);
        if (cached != null && now < cached.expireAt) {
            return Mono.just(cached.info);
        }

        return Mono.fromCallable(() -> {
            // 모의/실 혼용 방지: 한국투자증권 키만 사용. 모의(1) 우선, 없으면 실거래(0).
            Optional<UserApiKey> keyOpt = userApiKeyRepository
                    .findByUserIdAndBrokerTypeAndServerType(userId, BrokerType.KOREA_INVESTMENT, "1");
            if (keyOpt.isEmpty()) {
                keyOpt = userApiKeyRepository
                        .findByUserIdAndBrokerTypeAndServerType(userId, BrokerType.KOREA_INVESTMENT, "0");
            }
            UserApiKey userApiKey = keyOpt.orElseThrow(() -> {
                log.warn("[주식현재가] 한국투자증권 API 키 없음 userId={} (TB_USER_API_KEYS에 brokerType=KOREA_INVESTMENT, serverType=1 또는 0 등록 필요)", LogMaskingUtil.maskUserId(userId));
                return new IllegalStateException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId);
            });
            String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";
            log.info("[주식현재가] 토큰 조회/캐시 사용 userId={} serverType={}", LogMaskingUtil.maskUserId(userId), serverType);

            // DB에서 토큰 조회(없거나 만료 시 토큰 서비스 내부에서만 1회 발급). 직접 발급 호출 없음.
            String accessToken = tokenService.getAccessToken(userId, serverType);

            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

            Map<String, String> tokenInfo = new HashMap<>();
            tokenInfo.put("token", accessToken);
            tokenInfo.put("serverType", serverType);
            tokenInfo.put("appKey", appKey);
            tokenInfo.put("appSecret", appSecret);
            tokenInfo.put("userId", userId);

            tokenInfoCache.put(userId, new CachedTokenInfo(tokenInfo, now + TOKEN_INFO_CACHE_TTL_MS));
            return tokenInfo;
        });
    }

    /** 401 등으로 토큰 무효화 시 캐시 제거 (다음 호출에서 재발급) */
    private void invalidateTokenCache(String userId) {
        if (userId != null) {
            tokenInfoCache.remove(userId);
        }
    }

    /**
     * 차트 데이터 조회
     * 한국투자증권 API의 주식 차트 조회 API를 사용합니다.
     * Rate Limiter 적용: 실전투자 1초당 20건, 모의투자 1초당 2건
     * tokenInfo: token, serverType, appKey, appSecret, userId (ensureAccessToken 결과, N+1 방지)
     */
    private Mono<List<Map<String, Object>>> getChartData(String stockCode, String interval,
            Map<String, String> tokenInfo) {
        String accessToken = tokenInfo.get("token");
        String serverType = tokenInfo.get("serverType");
        String appKey = tokenInfo.get("appKey");
        String appSecret = tokenInfo.get("appSecret");
        String userId = tokenInfo.get("userId");

        String baseUrl = getBaseUrl(serverType);

        String trId = getDomesticQuotationTrId("03010100", serverType); // 주식 일봉차트 조회

        // 날짜 설정 (최근 200일)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(200);
        String endDateStr = endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String startDateStr = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 조회 파라미터 (GET query parameter로 전달)
        Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createMarketDataRequestBody(
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", "J", // J: 주식, ETF, ETN
                        "FID_INPUT_ISCD", stockCode, // 종목코드
                        "FID_INPUT_DATE_1", startDateStr, // 시작일자
                        "FID_INPUT_DATE_2", endDateStr, // 종료일자
                        "FID_PERIOD_DIV_CODE", convertIntervalToPeriodCode(interval), // 기간분할코드
                        "FID_ORG_ADJ_PRC", "0" // 수정주가 원주가 가격 여부 (0: 수정주가, 1: 원주가)
                ));
        URI uri = buildUriWithQueryParams(baseUrl, "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice",
                queryParams);

        // 요청 헤더: 공통 + custtype, hashkey (명세 준수)
        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);
        headers.set("custtype", "P");
        Map<String, Object> chartParamsForHash = new HashMap<>(queryParams);
        headers.set("hashkey", hashkeyUtil.generateHashkey(chartParamsForHash, appSecret));

        String path = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
        KoreaInvestmentApiLogging.logRequest("주식일봉차트", path, trId, queryParams);

        // Rate Limiter 적용 (실전투자: 1초당 20건, 모의투자: 1초당 2건)
        RateLimiter rateLimiter = getApiRateLimiter(serverType);

        return Mono.fromCallable(() -> {
            rateLimiter.acquirePermission();
            return null;
        })
                .flatMap(ignored -> webClient.get()
                        .uri(uri)
                        .headers(h -> h.addAll(headers))
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofMillis(properties.getTimeout()))
                        .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                .filter(throwable -> {
                                    if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                        org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                        if (ex.getStatusCode().value() == 401) {
                                            log.warn("401 에러 발생, 토큰 캐시 무효화 후 재시도: userId={}", LogMaskingUtil.maskUserId(userId));
                                            invalidateTokenCache(userId);
                                            return true;
                                        }
                                        return ex.getStatusCode().is5xxServerError();
                                    }
                                    return false;
                                }))
                        .map(response -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> responseMap = (Map<String, Object>) response;

                            String rtCd = (String) responseMap.get("rt_cd");
                            if (rtCd == null || !"0".equals(rtCd)) {
                                String msg1 = (String) responseMap.get("msg1");
                                String msgCd = (String) responseMap.get("msg_cd");
                                KoreaInvestmentApiLogging.logResponseError("주식일봉차트", 200, rtCd, msgCd, msg1, null);
                                throw new IllegalStateException(
                                        String.format("API 호출 실패: rt_cd=%s, msg_cd=%s, msg1=%s", rtCd, msgCd, msg1));
                            }

                            KoreaInvestmentApiLogging.logResponseSuccessFromMap("주식일봉차트", 200, responseMap);

                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> chartData = (List<Map<String, Object>>) responseMap.get("output2");
                            if (chartData == null) {
                                KoreaInvestmentApiLogging.logResponseError("주식일봉차트", 200, rtCd, null, "output2 없음", null);
                                return List.<Map<String, Object>>of();
                            }
                            return chartData;
                        }))
                .onErrorMap(error -> {
                    KoreaInvestmentApiLogging.logFailure("주식일봉차트", error);
                    return new IllegalStateException("차트 데이터 조회 실패", error);
                });
    }

    /**
     * 차트 데이터로부터 지표 계산
     */
    private IndicatorResponse calculateIndicator(List<Map<String, Object>> chartData, String indicator) {
        if (chartData == null || chartData.isEmpty()) {
            return createErrorResponse("차트 데이터가 없습니다");
        }

        // 차트 데이터를 숫자 배열로 변환 (null 체크 강화)
        double[] closes = chartData.stream()
                .map(d -> {
                    Object value = d.get("stck_clpr");
                    if (value == null) {
                        throw new IllegalStateException("차트 데이터에 종가(stck_clpr)가 없습니다");
                    }
                    return Double.parseDouble(value.toString());
                })
                .mapToDouble(Double::doubleValue)
                .toArray();
        double[] volumes = chartData.stream()
                .map(d -> {
                    Object value = d.get("acml_vol");
                    if (value == null) {
                        throw new IllegalStateException("차트 데이터에 거래량(acml_vol)이 없습니다");
                    }
                    return Double.parseDouble(value.toString());
                })
                .mapToDouble(Double::doubleValue)
                .toArray();
        double[] highs = chartData.stream()
                .map(d -> {
                    Object value = d.get("stck_hgpr");
                    if (value == null) {
                        throw new IllegalStateException("차트 데이터에 고가(stck_hgpr)가 없습니다");
                    }
                    return Double.parseDouble(value.toString());
                })
                .mapToDouble(Double::doubleValue)
                .toArray();
        double[] lows = chartData.stream()
                .map(d -> {
                    Object value = d.get("stck_lwpr");
                    if (value == null) {
                        throw new IllegalStateException("차트 데이터에 저가(stck_lwpr)가 없습니다");
                    }
                    return Double.parseDouble(value.toString());
                })
                .mapToDouble(Double::doubleValue)
                .toArray();

        IndicatorResponse.IndicatorResponseBuilder builder = IndicatorResponse.builder();

        switch (indicator.toLowerCase()) {
            case "rsi":
                double rsi = calculateRSI(closes, 14);
                builder.value(BigDecimal.valueOf(rsi));
                break;
            case "macd":
                double[] macdResult = calculateMACD(closes);
                builder.valueMacd(BigDecimal.valueOf(macdResult[0]));
                builder.valueMacdSignal(BigDecimal.valueOf(macdResult[1]));
                builder.valueMacdHist(BigDecimal.valueOf(macdResult[2]));
                break;
            case "ema":
                double ema20 = calculateEMA(closes, 20);
                double ema60 = calculateEMA(closes, 60);
                double ema120 = calculateEMA(closes, 120);
                builder.values(new BigDecimal[] {
                        BigDecimal.valueOf(ema20),
                        BigDecimal.valueOf(ema60),
                        BigDecimal.valueOf(ema120)
                });
                break;
            case "bbands":
                double[] bbResult = calculateBollingerBands(closes, 20, 2);
                builder.valueUpperBand(BigDecimal.valueOf(bbResult[0]));
                builder.valueMiddleBand(BigDecimal.valueOf(bbResult[1]));
                builder.valueLowerBand(BigDecimal.valueOf(bbResult[2]));
                break;
            case "atr":
                double atr = calculateATR(highs, lows, closes, 14);
                builder.valueAtr(BigDecimal.valueOf(atr));
                break;
            case "vwap":
                double vwap = calculateVWAP(closes, volumes);
                builder.value(BigDecimal.valueOf(vwap));
                break;
            default:
                return createErrorResponse("지원하지 않는 지표: " + indicator);
        }

        return builder.build();
    }

    /**
     * RSI 계산
     */
    private double calculateRSI(double[] closes, int period) {
        if (closes.length < period + 1) {
            return 50.0; // 기본값
        }

        double[] gains = new double[closes.length - 1];
        double[] losses = new double[closes.length - 1];

        for (int i = 1; i < closes.length; i++) {
            double change = closes[i] - closes[i - 1];
            gains[i - 1] = change > 0 ? change : 0;
            losses[i - 1] = change < 0 ? -change : 0;
        }

        double avgGain = 0;
        double avgLoss = 0;

        for (int i = 0; i < period; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        avgGain /= period;
        avgLoss /= period;

        for (int i = period; i < gains.length; i++) {
            avgGain = (avgGain * (period - 1) + gains[i]) / period;
            avgLoss = (avgLoss * (period - 1) + losses[i]) / period;
        }

        if (avgLoss == 0) {
            return 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * MACD 계산
     */
    private double[] calculateMACD(double[] closes) {
        double ema12 = calculateEMA(closes, 12);
        double ema26 = calculateEMA(closes, 26);
        double macd = ema12 - ema26;

        // Signal은 MACD의 9일 EMA (간단화를 위해 MACD 값 사용)
        double signal = macd * 0.9; // 근사값
        double hist = macd - signal;

        return new double[] { macd, signal, hist };
    }

    /**
     * EMA 계산
     */
    private double calculateEMA(double[] closes, int period) {
        if (closes.length < period) {
            return closes[closes.length - 1];
        }

        double multiplier = 2.0 / (period + 1);
        double ema = closes[0];

        for (int i = 1; i < closes.length; i++) {
            ema = (closes[i] * multiplier) + (ema * (1 - multiplier));
        }

        return ema;
    }

    /**
     * Bollinger Bands 계산
     */
    private double[] calculateBollingerBands(double[] closes, int period, double numStdDev) {
        if (closes.length < period) {
            double price = closes[closes.length - 1];
            return new double[] { price, price, price };
        }

        // SMA 계산
        double sum = 0;
        for (int i = closes.length - period; i < closes.length; i++) {
            sum += closes[i];
        }
        double sma = sum / period;

        // 표준편차 계산
        double variance = 0;
        for (int i = closes.length - period; i < closes.length; i++) {
            variance += Math.pow(closes[i] - sma, 2);
        }
        double stdDev = Math.sqrt(variance / period);

        double upper = sma + (numStdDev * stdDev);
        double lower = sma - (numStdDev * stdDev);

        return new double[] { upper, sma, lower };
    }

    /**
     * ATR 계산
     */
    private double calculateATR(double[] highs, double[] lows, double[] closes, int period) {
        if (highs.length < period + 1) {
            return 0;
        }

        double[] trueRanges = new double[highs.length - 1];
        for (int i = 1; i < highs.length; i++) {
            double tr1 = highs[i] - lows[i];
            double tr2 = Math.abs(highs[i] - closes[i - 1]);
            double tr3 = Math.abs(lows[i] - closes[i - 1]);
            trueRanges[i - 1] = Math.max(tr1, Math.max(tr2, tr3));
        }

        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += trueRanges[i];
        }

        return sum / period;
    }

    /**
     * VWAP 계산
     */
    private double calculateVWAP(double[] closes, double[] volumes) {
        if (closes.length != volumes.length || closes.length == 0) {
            return closes.length > 0 ? closes[closes.length - 1] : 0;
        }

        double totalValue = 0;
        double totalVolume = 0;

        for (int i = 0; i < closes.length; i++) {
            totalValue += closes[i] * volumes[i];
            totalVolume += volumes[i];
        }

        return totalVolume > 0 ? totalValue / totalVolume : closes[closes.length - 1];
    }

    /**
     * interval을 한국투자증권 API의 기간분할코드로 변환
     */
    private String convertIntervalToPeriodCode(String interval) {
        if (interval == null) {
            return "D"; // 일봉
        }

        String lowerInterval = interval.toLowerCase();

        if (lowerInterval.equals("1d") || lowerInterval.equals("1day")) {
            return "D"; // 일봉
        }
        if (lowerInterval.equals("1w") || lowerInterval.equals("1week")) {
            return "W"; // 주봉
        }
        if (lowerInterval.equals("1m") || lowerInterval.equals("1month")) {
            return "M"; // 월봉
        }

        return "D"; // 기본값: 일봉
    }

    /**
     * Base URL 가져오기 (실거래/모의투자)
     */
    private String getBaseUrl(String serverType) {
        if ("1".equals(serverType)) {
            return BASE_URL_VIRTUAL; // 모의투자
        }
        return BASE_URL_REAL; // 실거래
    }

    /**
     * 주식현재가 시세 API TR_ID (inquire-price).
     * 공식 샘플(open-trading-api domestic_stock_functions.py inquire_price)은 실전/모의 모두 FHKST01010100 사용.
     * 실전에 FHPST01010100을 쓰면 한투 API가 404를 반환할 수 있으므로 여기서는 FHKST01010100 통일.
     */
    private String getCurrentPriceTrId(String serverType) {
        return "FHKST01010100";
    }

    /**
     * 국내주식 시세 API TR_ID (실전 FHPST / 모의 FHKST 구분)
     * 한국투자증권: 실전 도메인은 FHPST, 모의 도메인은 FHKST 접두사 사용.
     *
     * @param suffix TR_ID 접미사 (예: "01010100" 주식현재가, "03010100" 일봉차트)
     * @param serverType "0" 실거래, "1" 모의투자
     */
    private String getDomesticQuotationTrId(String suffix, String serverType) {
        if ("0".equals(serverType)) {
            return "FHPST" + suffix; // 실전
        }
        return "FHKST" + suffix; // 모의
    }

    /**
     * 에러 응답 생성
     */
    private IndicatorResponse createErrorResponse(String error) {
        return IndicatorResponse.builder()
                .error(error)
                .build();
    }
}
