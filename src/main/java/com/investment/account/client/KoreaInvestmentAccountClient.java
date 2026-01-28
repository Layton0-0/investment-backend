package com.investment.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BuyableAmountDto;
import com.investment.account.dto.SellableQuantityDto;
import com.investment.account.dto.OrderHistoryDto;
import com.investment.account.dto.AccountAssetDto;
import com.investment.account.dto.ProfitLossDto;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.KoreaInvestmentRequestBuilder;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.investment.account.client.KoreaInvestmentAccountApiConstants.*;

/**
 * 한국투자증권 계좌 API 클라이언트
 * 
 * 한국투자증권 Open API를 사용하여 계좌 관련 정보를 조회합니다.
 * - 주식잔고조회
 * - 매수가능조회
 * - 매도가능수량조회
 * - 주문체결조회
 * - 투자계좌자산현황조회
 * - 기간별손익조회
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KoreaInvestmentAccountClient {

    private final WebClient webClient;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final EncryptionUtil encryptionUtil;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    // Rate Limiter 인스턴스 이름
    private static final String RATE_LIMITER_API_VIRTUAL = "koreaInvestmentApi";
    private static final String RATE_LIMITER_API_REAL = "koreaInvestmentApiReal";

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
            try {
                log.info("=== 한국투자증권 API 요청: {} ===", apiName);
                log.info("요청 URL: {}", uri.toString());
                log.info("요청 헤더:");
                headers.forEach((name, values) -> {
                    if ("authorization".equalsIgnoreCase(name)) {
                        // Authorization 헤더는 마스킹 처리
                        log.info("  {}: Bearer ***", name);
                    } else {
                        log.info("  {}: {}", name, values);
                    }
                });
                log.info("요청 바디 (JSON):");
                String jsonBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(requestBody);
                log.info("{}", jsonBody);
                log.info("=== 요청 종료 ===");
            } catch (Exception e) {
                log.warn("API 요청 로그 출력 실패", e);
            }
        }
    }

    /**
     * local 환경에서 API 응답 상세 로그 출력
     */
    private void logApiResponse(String apiName, String responseJson) {
        if (isLocalProfile()) {
            try {
                log.info("=== 한국투자증권 API 응답: {} ===", apiName);
                if (responseJson != null && !responseJson.isEmpty()) {
                    // JSON 포맷팅
                    JsonNode jsonNode = objectMapper.readTree(responseJson);
                    String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
                    log.info("응답 바디 (JSON):");
                    log.info("{}", formattedJson);
                } else {
                    log.info("응답 바디: (null 또는 빈 문자열)");
                }
                log.info("=== 응답 종료 ===");
            } catch (Exception e) {
                log.warn("API 응답 로그 출력 실패", e);
                // JSON 파싱 실패 시 원본 문자열 출력
                log.info("응답 바디 (원본): {}", responseJson);
            }
        }
    }

    /**
     * API 호출용 Rate Limiter 가져오기
     */
    private RateLimiter getApiRateLimiter(String serverType) {
        if ("0".equals(serverType)) {
            return rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_REAL);
        }
        return rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_VIRTUAL);
    }

    /**
     * 주식잔고조회
     * 
     * @param userId    사용자 ID
     * @param accountNo 계좌번호 (빈 문자열이면 기본 계좌)
     * @return AccountBalanceDto와 보유 종목 목록
     */
    public BalanceAndPositionsResult inquireBalance(String userId, String accountNo) {
        return inquireBalance(userId, accountNo, null);
    }

    /**
     * 주식잔고조회 (토큰 직접 전달)
     * 
     * @param userId      사용자 ID
     * @param accountNo   계좌번호 (빈 문자열이면 기본 계좌)
     * @param accessToken Access Token (null이면 자동 조회)
     * @return AccountBalanceDto와 보유 종목 목록
     */
    public BalanceAndPositionsResult inquireBalance(String userId, String accountNo, String accessToken) {
        log.debug("주식잔고조회: userId={}, accountNo={}", userId, accountNo);

        try {
            // 사용자 API 키 조회
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            // Access Token 조회 (제공되지 않은 경우에만)
            if (accessToken == null) {
                accessToken = tokenService.getAccessToken(userId);
            }

            // API 키 복호화
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            // Base URL 및 TR ID 결정
            String baseUrl = getBaseUrl(serverType);
            String trId = getBalanceTrId(serverType);

            // API 엔드포인트
            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(PATH_INQUIRE_BALANCE)
                    .build()
                    .toUri();

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 요청 바디 생성 (공통 파라미터 + API별 고유 파라미터)
            Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo != null ? accountNo : "",
                    Map.of(
                            "AFHR_FLPR_YN", "N", // 시간외단일가여부
                            "OFL_YN", "", // 오프라인여부
                            "INQR_DVSN", "02", // 조회구분 (02: 종목별)
                            "UNPR_DVSN", "01", // 단가구분 (01: 기본)
                            "FUND_STTL_ICLD_YN", "N", // 펀드결제분포함여부
                            "FNCG_AMT_AUTO_RDPT_YN", "N", // 융자금액자동상환여부
                            "PRCS_DVSN", "01", // 처리구분 (01: 기본)
                            "CTX_AREA_FK100", "", // 연속조회검색조건100
                            "CTX_AREA_NK100", "" // 연속조회키100
                    ));

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("주식잔고조회", uri, headers, requestBody);

            // Rate Limiter 적용
            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            // API 호출
            String responseJson = webClient.post()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(throwable -> {
                                if (throwable instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                                    org.springframework.web.reactive.function.client.WebClientResponseException ex = (org.springframework.web.reactive.function.client.WebClientResponseException) throwable;
                                    if (ex.getStatusCode().value() == 401) {
                                        log.warn("401 에러 발생, 토큰 재발급 시도: userId={}", userId);
                                        try {
                                            List<UserApiKey> apiKeys = userApiKeyRepository.findByUserId(userId);
                                            if (!apiKeys.isEmpty()) {
                                                tokenService.issueTokenForUser(apiKeys.get(0));
                                            }
                                        } catch (Exception e) {
                                            log.error("토큰 재발급 실패: userId={}", userId, e);
                                        }
                                        return true;
                                    }
                                    return ex.getStatusCode().is5xxServerError();
                                }
                                return false;
                            }))
                    .block();

            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }

            // local 환경에서 응답 상세 로그 출력
            logApiResponse("주식잔고조회", responseJson);

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(responseJson);

            // 응답 코드 확인
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }

            // output (계좌 잔고 정보)
            JsonNode output = rootNode.path("output");
            AccountBalanceDto balanceDto = parseBalanceOutput(output, accountNo);

            // output1 (보유 종목 목록)
            JsonNode output1 = rootNode.path("output1");
            List<AccountPositionDto> positions = parsePositionsOutput(output1);

            return new BalanceAndPositionsResult(balanceDto, positions);

        } catch (DomainException e) {
            // DomainException은 그대로 전파하되, 로깅 추가
            log.warn("주식잔고조회 DomainException: userId={}, accountNo={}, error={}",
                    userId, accountNo, e.getMessage());
            throw e;
        } catch (Exception e) {
            // 일반 Exception은 상세 로깅 후 DomainException으로 변환
            log.error("주식잔고조회 실패: userId={}, accountNo={}", userId, accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "주식잔고조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 매수가능조회
     */
    public BuyableAmountDto inquireBuyableAmount(String userId, String accountNo, String symbol, BigDecimal price) {
        log.debug("매수가능조회: userId={}, accountNo={}, symbol={}, price={}", userId, accountNo, symbol, price);

        try {
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getBuyableTrId(serverType);

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(PATH_INQUIRE_PSBL_ORDER)
                    .build()
                    .toUri();

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 요청 바디 생성 (공통 파라미터 + API별 고유 파라미터)
            Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "PDNO", symbol,
                            "ORD_DVSN", "01", // 주문구분 (01: 시장가)
                            "ORD_UNPR", price.toPlainString(),
                            "CMA_EVLU_AMT_ICLD_YN", "N", // CMA평가금액포함여부
                            "OVRS_ICLD_YN", "N" // 해외포함여부
                    ));

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("매수가능조회", uri, headers, requestBody);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.post()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }

            // local 환경에서 응답 상세 로그 출력
            logApiResponse("매수가능조회", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }

            JsonNode output = rootNode.path("output");
            return BuyableAmountDto.builder()
                    .accountNo(accountNo)
                    .symbol(symbol)
                    .price(price)
                    .buyableAmount(new BigDecimal(output.path("ord_psbl_cash").asText("0")))
                    .buyableQuantity(Integer.parseInt(output.path("ord_psbl_qty").asText("0")))
                    .currency("KRW")
                    .build();

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("매수가능조회 실패: userId={}, accountNo={}, symbol={}", userId, accountNo, symbol, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "매수가능조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 매도가능수량조회
     */
    public SellableQuantityDto inquireSellableQuantity(String userId, String accountNo, String symbol) {
        log.debug("매도가능수량조회: userId={}, accountNo={}, symbol={}", userId, accountNo, symbol);

        try {
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getSellableTrId(serverType);

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(PATH_INQUIRE_PSBL_ORDER2)
                    .build()
                    .toUri();

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 요청 바디 생성 (공통 파라미터 + API별 고유 파라미터)
            Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "PDNO", symbol,
                            "ORD_DVSN", "01" // 주문구분 (01: 지정가)
                    ));

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("매도가능수량조회", uri, headers, requestBody);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.post()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }

            // local 환경에서 응답 상세 로그 출력
            logApiResponse("매도가능수량조회", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }

            JsonNode output = rootNode.path("output");
            return SellableQuantityDto.builder()
                    .accountNo(accountNo)
                    .symbol(symbol)
                    .sellableQuantity(Integer.parseInt(output.path("ord_psbl_qty").asText("0")))
                    .holdingQuantity(Integer.parseInt(output.path("hldg_qty").asText("0")))
                    .averagePrice(new BigDecimal(output.path("pchs_avg_pric").asText("0")))
                    .currency("KRW")
                    .build();

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("매도가능수량조회 실패: userId={}, accountNo={}, symbol={}", userId, accountNo, symbol, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "매도가능수량조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문체결조회
     */
    public List<OrderHistoryDto> inquireOrderHistory(String userId, String accountNo, LocalDate startDate,
            LocalDate endDate) {
        log.debug("주문체결조회: userId={}, accountNo={}, startDate={}, endDate={}", userId, accountNo, startDate, endDate);

        try {
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getOrderHistoryTrId(serverType);

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(PATH_INQUIRE_DAILY_CCLD)
                    .build()
                    .toUri();

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 요청 바디 생성 (공통 파라미터 + API별 고유 파라미터)
            // Map.of()는 최대 10개 키-값 쌍만 지원하므로 HashMap 사용
            Map<String, String> additionalParams = new HashMap<>();
            additionalParams.put("INQR_STRT_DT", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            additionalParams.put("INQR_END_DT", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            additionalParams.put("SLL_BUY_DVSN_CD", "00"); // 00: 전체, 01: 매도, 02: 매수
            additionalParams.put("INQR_DVSN", "00"); // 00: 역순, 01: 정순
            additionalParams.put("PDNO", ""); // 종목코드 (빈 값이면 전체)
            additionalParams.put("CCLD_DVSN", "00"); // 00: 전체, 01: 체결, 02: 미체결
            additionalParams.put("ORD_GNO_BRNO", ""); // 주문채번지점번호
            additionalParams.put("ODNO", ""); // 주문번호
            additionalParams.put("INQR_DVSN_3", "00"); // 조회구분3 (00: 전체, 01: 현금, 02: 신용, ...)
            additionalParams.put("INQR_DVSN_1", ""); // 조회구분1
            additionalParams.put("EXCG_ID_DVSN_CD", "KRX"); // 거래소ID구분코드 (KRX: 한국거래소)
            additionalParams.put("CTX_AREA_FK100", ""); // 연속조회검색조건100
            additionalParams.put("CTX_AREA_NK100", ""); // 연속조회키100
            Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo, additionalParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("주문체결조회", uri, headers, requestBody);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.post()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }

            // local 환경에서 응답 상세 로그 출력
            logApiResponse("주문체결조회", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }

            JsonNode output1 = rootNode.path("output1");
            List<OrderHistoryDto> orderHistoryList = new ArrayList<>();

            if (output1.isArray()) {
                for (JsonNode item : output1) {
                    OrderHistoryDto dto = OrderHistoryDto.builder()
                            .accountNo(accountNo)
                            .symbol(item.path("pdno").asText())
                            .orderNo(item.path("odno").asText())
                            .orderType(item.path("sll_buy_dvsn_cd").asText("02").equals("01") ? "SELL" : "BUY")
                            .orderQuantity(Integer.parseInt(item.path("ord_qty").asText("0")))
                            .orderPrice(new BigDecimal(item.path("ord_unpr").asText("0")))
                            .executedQuantity(Integer.parseInt(item.path("tot_ccld_qty").asText("0")))
                            .executedPrice(new BigDecimal(item.path("avg_prvs").asText("0")))
                            .orderStatus(item.path("ord_stat_cd").asText())
                            .orderTime(parseDateTime(item.path("ord_tmd").asText(), item.path("ord_dt").asText()))
                            .executedTime(parseDateTime(item.path("exec_tmd").asText(), item.path("exec_dt").asText()))
                            .currency("KRW")
                            .build();
                    orderHistoryList.add(dto);
                }
            }

            return orderHistoryList;

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("주문체결조회 실패: userId={}, accountNo={}", userId, accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "주문체결조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 투자계좌자산현황조회
     */
    public AccountAssetDto inquireAssets(String userId, String accountNo) {
        log.debug("투자계좌자산현황조회: userId={}, accountNo={}", userId, accountNo);

        try {
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getAssetsTrId(serverType);

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(PATH_INQUIRE_ASSETS)
                    .build()
                    .toUri();

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 요청 바디 생성 (공통 파라미터 + API별 고유 파라미터)
            Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "AFHR_FLPR_YN", "N",
                            "OFL_YN", "",
                            "INQR_DVSN", "02",
                            "UNPR_DVSN", "01",
                            "FUND_STTL_ICLD_YN", "N",
                            "FNCG_AMT_AUTO_RDPT_YN", "N",
                            "PRCS_DVSN", "01",
                            "CTX_AREA_FK100", "",
                            "CTX_AREA_NK100", ""));

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("투자계좌자산현황조회", uri, headers, requestBody);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.post()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }

            // local 환경에서 응답 상세 로그 출력
            logApiResponse("투자계좌자산현황조회", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }

            JsonNode output = rootNode.path("output");
            return AccountAssetDto.builder()
                    .accountNo(accountNo)
                    .totalAssetValue(new BigDecimal(output.path("tot_evlu_amt").asText("0")))
                    .deposit(new BigDecimal(output.path("dnca_tot_amt").asText("0")))
                    .stockValue(new BigDecimal(output.path("scts_evlu_amt").asText("0")))
                    .totalProfitLoss(new BigDecimal(output.path("evlu_pfls_smtl_amt").asText("0")))
                    .totalProfitLossRate(new BigDecimal(output.path("evlu_pfls_rt").asText("0")))
                    .orderableCash(new BigDecimal(output.path("ord_psbl_cash").asText("0")))
                    .currency("KRW")
                    .build();

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("투자계좌자산현황조회 실패: userId={}, accountNo={}", userId, accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "투자계좌자산현황조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기간별손익조회
     */
    public ProfitLossDto inquirePeriodProfitLoss(String userId, String accountNo, LocalDate startDate,
            LocalDate endDate) {
        log.debug("기간별손익조회: userId={}, accountNo={}, startDate={}, endDate={}", userId, accountNo, startDate, endDate);

        try {
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getPeriodProfitLossTrId(serverType);

            URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .path(PATH_INQUIRE_PERIOD_PROFIT_LOSS)
                    .build()
                    .toUri();

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 요청 바디 생성 (공통 파라미터 + API별 고유 파라미터)
            Map<String, String> requestBody = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "INQR_STRT_DT", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            "INQR_END_DT", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            "SLL_BUY_DVSN_CD", "00", // 00: 전체
                            "INQR_DVSN", "00", // 00: 역순
                            "CTX_AREA_FK100", "",
                            "CTX_AREA_NK100", ""));

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("기간별손익조회", uri, headers, requestBody);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.post()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }

            // local 환경에서 응답 상세 로그 출력
            logApiResponse("기간별손익조회", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }

            JsonNode output = rootNode.path("output");
            JsonNode output1 = rootNode.path("output1");

            List<ProfitLossDto.DailyProfitLossDto> dailyList = new ArrayList<>();
            if (output1.isArray()) {
                for (JsonNode item : output1) {
                    ProfitLossDto.DailyProfitLossDto daily = ProfitLossDto.DailyProfitLossDto.builder()
                            .date(LocalDate.parse(item.path("trd_dd").asText(),
                                    DateTimeFormatter.ofPattern("yyyyMMdd")))
                            .profitLoss(new BigDecimal(item.path("evlu_pfls_amt").asText("0")))
                            .profitLossRate(new BigDecimal(item.path("evlu_pfls_rt").asText("0")))
                            .build();
                    dailyList.add(daily);
                }
            }

            return ProfitLossDto.builder()
                    .accountNo(accountNo)
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalProfitLoss(new BigDecimal(output.path("evlu_pfls_smtl_amt").asText("0")))
                    .totalProfitLossRate(new BigDecimal(output.path("evlu_pfls_rt").asText("0")))
                    .realizedProfitLoss(new BigDecimal(output.path("rlz_pfls_amt").asText("0")))
                    .unrealizedProfitLoss(new BigDecimal(output.path("evlu_pfls_amt").asText("0")))
                    .dailyProfitLossList(dailyList)
                    .currency("KRW")
                    .build();

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("기간별손익조회 실패: userId={}, accountNo={}", userId, accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "기간별손익조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 계좌 잔고 정보 파싱
     */
    private AccountBalanceDto parseBalanceOutput(JsonNode output, String accountNo) {
        if (output == null || output.isNull()) {
            return AccountBalanceDto.builder()
                    .accountNo(accountNo)
                    .totalBalance(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .investedAmount(BigDecimal.ZERO)
                    .currency("KRW")
                    .deposit(BigDecimal.ZERO)
                    .orderableCash(BigDecimal.ZERO)
                    .totalAssetValue(BigDecimal.ZERO)
                    .totalProfitLoss(BigDecimal.ZERO)
                    .totalProfitLossRate(BigDecimal.ZERO)
                    .build();
        }

        BigDecimal deposit = new BigDecimal(output.path("dnca_tot_amt").asText("0"));
        BigDecimal orderableCash = new BigDecimal(output.path("ord_psbl_cash").asText("0"));
        BigDecimal totalAssetValue = new BigDecimal(output.path("tot_evlu_amt").asText("0"));
        BigDecimal totalProfitLoss = new BigDecimal(output.path("evlu_pfls_smtl_amt").asText("0"));
        BigDecimal totalProfitLossRate = new BigDecimal(output.path("evlu_pfls_rt").asText("0"));

        return AccountBalanceDto.builder()
                .accountNo(accountNo)
                .totalBalance(totalAssetValue) // 총 평가금액
                .availableBalance(orderableCash) // 주문가능금액
                .investedAmount(deposit) // 예수금
                .currency("KRW")
                .deposit(deposit)
                .orderableCash(orderableCash)
                .totalAssetValue(totalAssetValue)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossRate(totalProfitLossRate)
                .build();
    }

    /**
     * 보유 종목 목록 파싱
     */
    private List<AccountPositionDto> parsePositionsOutput(JsonNode output1) {
        List<AccountPositionDto> positions = new ArrayList<>();

        if (output1 == null || !output1.isArray()) {
            return positions;
        }

        for (JsonNode item : output1) {
            String symbol = item.path("pdno").asText();
            String name = item.path("prdt_name").asText();
            Integer quantity = Integer.parseInt(item.path("hldg_qty").asText("0"));
            BigDecimal averagePrice = new BigDecimal(item.path("pchs_avg_pric").asText("0"));
            BigDecimal currentPrice = new BigDecimal(item.path("prpr").asText("0"));
            BigDecimal purchaseAmount = new BigDecimal(item.path("pchs_amt").asText("0"));
            BigDecimal evaluationAmount = new BigDecimal(item.path("evlu_amt").asText("0"));
            BigDecimal evaluationProfitLoss = new BigDecimal(item.path("evlu_pfls_amt").asText("0"));
            BigDecimal evaluationProfitLossRate = new BigDecimal(item.path("evlu_pfls_rt").asText("0"));

            AccountPositionDto position = AccountPositionDto.builder()
                    .symbol(symbol)
                    .name(name)
                    .quantity(quantity)
                    .averagePrice(averagePrice)
                    .currentPrice(currentPrice)
                    .totalValue(evaluationAmount)
                    .profitLoss(evaluationProfitLoss)
                    .profitLossRate(evaluationProfitLossRate)
                    .currency("KRW")
                    .lastUpdated(LocalDateTime.now())
                    .purchaseAmount(purchaseAmount)
                    .evaluationAmount(evaluationAmount)
                    .evaluationProfitLoss(evaluationProfitLoss)
                    .evaluationProfitLossRate(evaluationProfitLossRate)
                    .holdingQuantity(quantity)
                    .build();

            positions.add(position);
        }

        return positions;
    }

    /**
     * 날짜와 시간 문자열을 LocalDateTime으로 변환
     */
    private LocalDateTime parseDateTime(String timeStr, String dateStr) {
        if (timeStr == null || timeStr.isEmpty() || dateStr == null || dateStr.isEmpty()) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            int hour = Integer.parseInt(timeStr.substring(0, 2));
            int minute = Integer.parseInt(timeStr.substring(2, 4));
            int second = Integer.parseInt(timeStr.substring(4, 6));
            return LocalDateTime.of(date, java.time.LocalTime.of(hour, minute, second));
        } catch (Exception e) {
            log.warn("날짜/시간 파싱 실패: dateStr={}, timeStr={}", dateStr, timeStr, e);
            return null;
        }
    }

    /**
     * 잔고와 보유 종목 결과를 담는 내부 클래스
     */
    public static class BalanceAndPositionsResult {
        private final AccountBalanceDto balance;
        private final List<AccountPositionDto> positions;

        public BalanceAndPositionsResult(AccountBalanceDto balance, List<AccountPositionDto> positions) {
            this.balance = balance;
            this.positions = positions;
        }

        public AccountBalanceDto getBalance() {
            return balance;
        }

        public List<AccountPositionDto> getPositions() {
            return positions;
        }
    }
}
