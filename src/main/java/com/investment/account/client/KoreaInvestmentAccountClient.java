package com.investment.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceRealizedProfitLossDto;
import com.investment.account.dto.BuyableAmountDto;
import com.investment.account.dto.SellableQuantityDto;
import com.investment.account.dto.OrderHistoryDto;
import com.investment.account.dto.CancelableOrderDto;
import com.investment.account.dto.AccountAssetDto;
import com.investment.account.dto.OverseasBalanceSummaryDto;
import com.investment.account.dto.PeriodProfitLossStatusDto;
import com.investment.account.dto.ProfitLossDto;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserAccountRepository;
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
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
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
import java.util.Optional;

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
    private final UserAccountRepository userAccountRepository;
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
                    String valueToLog = (values != null && !values.isEmpty()) ? values.get(0) : "";
                    if ("authorization".equalsIgnoreCase(name)) {
                        log.info("  {}: Bearer ***", name);
                    } else if ("appkey".equalsIgnoreCase(name) || "app-key".equalsIgnoreCase(name)) {
                        log.info("  {}: {}", name, LogMaskingUtil.maskApiKey(valueToLog));
                    } else if ("appsecret".equalsIgnoreCase(name) || "app-secret".equalsIgnoreCase(name)) {
                        log.info("  {}: {}", name, LogMaskingUtil.maskSecret(valueToLog));
                    } else {
                        log.info("  {}: {}", name, values);
                    }
                });
                log.info("요청 파라미터 (query):");
                Map<String, String> maskedBody = new HashMap<>(requestBody);
                if (maskedBody.containsKey("CANO") && maskedBody.get("CANO") != null) {
                    maskedBody.put("CANO", LogMaskingUtil.maskAccountNo(maskedBody.get("CANO")));
                }
                String jsonBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(maskedBody);
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
     * 조회 API용 URI 생성 (GET + query parameter).
     * 한국투자증권 계좌/시세 조회 API는 GET 메서드에 query parameter로 전달한다.
     */
    private URI buildUriWithQueryParams(String baseUrl, String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        return builder.build().toUri();
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
     * 계좌번호에 해당하는 서버 타입 조회 (모의/실거래 구분)
     * UserAccount에서 userId·계좌번호 일치하는 계좌의 serverType 반환.
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
     * 계좌번호에 해당하는 API 키 조회 (모의/실거래 구분).
     * 해당 계좌의 serverType에 대한 API 키만 반환하며, 다른 serverType으로 fallback하지 않음.
     * 실거래 계좌가 저장되어 있지 않으면 빈 값을 반환하여 토큰 발급/API 요청을 하지 않음.
     */
    private Optional<UserApiKey> getUserApiKeyForAccount(String userId, String accountNo) {
        String serverType = resolveServerTypeForAccount(userId, accountNo);
        if (serverType == null) {
            return Optional.empty();
        }
        return userApiKeyRepository.findByUserIdAndBrokerTypeAndServerType(userId,
                BrokerType.KOREA_INVESTMENT, serverType);
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
        log.debug("주식잔고조회: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo));

        try {
            // 사용자 API 키 조회 (계좌번호에 맞는 서버 타입의 키 사용)
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));
            String serverType;

            // API 키 복호화 (선택된 키의 서버 타입으로 API 호출)
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            serverType = userApiKey.getServerType();

            // Access Token 조회 (해당 serverType만 사용, 실계좌 미저장 시 호출되지 않음)
            if (accessToken == null) {
                accessToken = tokenService.getAccessToken(userId, serverType);
            }

            // Base URL 및 TR ID 결정
            String baseUrl = getBaseUrl(serverType);
            String trId = getBalanceTrId(serverType);

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 조회 파라미터 (GET query parameter로 전달)
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo != null ? accountNo : "",
                    Map.of(
                            "AFHR_FLPR_YN", "N", // 시간외단일가여부
                            "OFL_YN", "", // 오프라인여부
                            "INQR_DVSN", "01", // 조회구분 (01: 대출일별, 02: 종목별는 2026-02-11 공지로 제한됨)
                            "UNPR_DVSN", "01", // 단가구분 (01: 기본)
                            "FUND_STTL_ICLD_YN", "N", // 펀드결제분포함여부
                            "FNCG_AMT_AUTO_RDPT_YN", "N", // 융자금액자동상환여부
                            "PRCS_DVSN", "01", // 처리구분 (01: 기본)
                            "CTX_AREA_FK100", "", // 연속조회검색조건100
                            "CTX_AREA_NK100", "" // 연속조회키100
                    ));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_BALANCE, queryParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("주식잔고조회", uri, headers, queryParams);

            // Rate Limiter 적용
            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            // API 호출 (GET + query parameter). 401 시 DB에서 토큰 재조회 후 1회만 재시도(직접 발급 없음)
            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                            .filter(t -> t instanceof WebClientResponseException
                                    && ((WebClientResponseException) t).getStatusCode().is5xxServerError()))
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        if (ex.getStatusCode().value() == 401) {
                            log.warn("401 에러 발생, DB에서 토큰 재조회 후 1회 재시도: userId={}", LogMaskingUtil.maskUserId(userId));
                            String newToken = tokenService.getAccessToken(userId, serverType);
                            HttpHeaders retryHeaders = KoreaInvestmentRequestBuilder.createCommonHeaders(
                                    newToken, appKey, appSecret, trId);
                            return webClient.get()
                                    .uri(uri)
                                    .headers(h -> h.addAll(retryHeaders))
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .timeout(Duration.ofSeconds(10))
                                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                                            .filter(t -> t instanceof WebClientResponseException
                                                    && ((WebClientResponseException) t).getStatusCode().is5xxServerError()));
                        }
                        return Mono.error(ex);
                    })
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

            // 계좌 잔고 정보: 한투 API는 output(단일) 또는 output2(배열/단일)로 반환. open-trading-api 샘플은 output1(보유종목)+output2(잔고요약).
            JsonNode output = rootNode.path("output");
            if (output == null || output.isNull() || !output.isObject()) {
                JsonNode output2 = rootNode.path("output2");
                if (output2 != null && output2.isArray() && output2.size() > 0) {
                    output = output2.get(0);
                } else if (output2 != null && output2.isObject()) {
                    output = output2;
                }
            }
            AccountBalanceDto balanceDto = parseBalanceOutput(output, accountNo);

            // output1 (보유 종목 목록)
            JsonNode output1 = rootNode.path("output1");
            List<AccountPositionDto> positions = parsePositionsOutput(output1);

            return new BalanceAndPositionsResult(balanceDto, positions);

        } catch (DomainException e) {
            // DomainException은 그대로 전파하되, 로깅 추가
            log.warn("주식잔고조회 DomainException: userId={}, accountNo={}, error={}",
                    LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), e.getMessage());
            throw e;
        } catch (Exception e) {
            // 일반 Exception은 상세 로깅 후 DomainException으로 변환
            log.error("주식잔고조회 실패: userId={}, accountNo={}", userId, accountNo, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "주식잔고조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 해외주식 현재잔고(체결기준) 조회 — 미국(840) 외화(02) 기준.
     * GET + query parameter. output1(보유종목) + output2(예수금·총자산 요약) 파싱.
     *
     * @param userId    사용자 ID
     * @param accountNo 계좌번호 (8-2 형식)
     * @return 보유 종목 목록과 요약(예수금·총자산). 실패 시 빈 목록과 null 요약, 예외 없음
     */
    public OverseasBalanceResult inquireOverseasBalance(String userId, String accountNo) {
        if (userId == null || accountNo == null || accountNo.trim().isEmpty()) {
            return new OverseasBalanceResult(new ArrayList<>(), null);
        }
        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo).orElse(null);
            if (userApiKey == null) {
                return new OverseasBalanceResult(new ArrayList<>(), null);
            }
            String accessToken = tokenService.getAccessToken(userId, userApiKey.getServerType());
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();
            String baseUrl = getBaseUrl(serverType);
            String trId = getOverseasBalanceTrId(serverType);

            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "WCRC_FRCR_DVSN_CD", "02", // 외화
                            "NATN_CD", "840",          // 미국
                            "TR_MKET_CD", "00",        // 전체
                            "INQR_DVSN_CD", "00"       // 전체
                    ));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_OVERSAS_INQUIRE_PRESENT_BALANCE, queryParams);

            logApiRequest("해외주식현재잔고조회", uri, headers, queryParams);
            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (responseJson == null) {
                return new OverseasBalanceResult(new ArrayList<>(), null);
            }
            logApiResponse("해외주식현재잔고조회", responseJson);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                log.warn("해외주식 잔고 조회 실패: rt_cd={}, msg1={}", rtCd, rootNode.path("msg1").asText(""));
                return new OverseasBalanceResult(new ArrayList<>(), null);
            }

            JsonNode output1 = rootNode.path("output1");
            List<AccountPositionDto> positions = parseOverseasPositionsOutput(output1);

            JsonNode output2 = rootNode.path("output2");
            OverseasBalanceSummaryDto summary = parseOverseasBalanceOutput2(output2);

            return new OverseasBalanceResult(positions, summary);
        } catch (Exception e) {
            log.debug("해외주식 잔고 조회 실패(스킵): accountNo={}, error={}",
                    LogMaskingUtil.maskAccountNo(accountNo), e.getMessage());
            return new OverseasBalanceResult(new ArrayList<>(), null);
        }
    }

    /**
     * 해외주식 잔고조회 API output2 파싱.
     * 한투 명세(해외주식 잔고조회): 예수금 pchs_amt(매수가능금액/현금예수금), 총자산 tot_ass_amt.
     * 응답 구조에 따라 fallback: frcr_dprs_amt, tot_dncl_amt / ovrs_tot_ast_amt, tot_asst_amt.
     */
    private OverseasBalanceSummaryDto parseOverseasBalanceOutput2(JsonNode output2) {
        if (output2 == null || output2.isNull()) {
            return null;
        }
        JsonNode node = output2.isArray() && output2.size() > 0 ? output2.get(0) : output2;
        if (!node.isObject()) {
            return null;
        }
        BigDecimal deposit = pathDecimal(node, "pchs_amt", "frcr_dprs_amt", "tot_dncl_amt");
        BigDecimal totalAsset = pathDecimal(node, "tot_ass_amt", "ovrs_tot_ast_amt", "tot_asst_amt");
        BigDecimal totalProfitLoss = pathDecimal(node, "tot_evlu_pfls_amt");
        BigDecimal totalProfitLossRate = pathDecimal(node, "evlu_erng_rt1", "evlu_pfls_rt");
        if (deposit == null) {
            deposit = BigDecimal.ZERO;
        }
        if (totalAsset == null) {
            totalAsset = BigDecimal.ZERO;
        }
        if (totalProfitLoss == null) {
            totalProfitLoss = BigDecimal.ZERO;
        }
        return OverseasBalanceSummaryDto.builder()
                .deposit(deposit)
                .totalAsset(totalAsset)
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossRate(totalProfitLossRate)
                .currency("USD")
                .build();
    }

    /**
     * 해외주식 현재잔고 응답의 보유 종목 목록 파싱.
     * output1 배열 사용. 필드명은 KIS 해외 API 스펙(ovrs_* 등)에 맞춤.
     */
    private List<AccountPositionDto> parseOverseasPositionsOutput(JsonNode output1) {
        List<AccountPositionDto> positions = new ArrayList<>();
        if (output1 == null || !output1.isArray()) {
            return positions;
        }
        for (JsonNode item : output1) {
            AccountPositionDto dto = parseOverseasPositionItem(item);
            if (dto != null) {
                positions.add(dto);
            }
        }
        return positions;
    }

    private AccountPositionDto parseOverseasPositionItem(JsonNode item) {
        try {
            String symbol = pathText(item, "pdno", "iscd", "ovrs_pdno");
            String name = pathText(item, "prdt_name", "ovrs_item_name", "item_name");
            int quantity = pathInt(item, "hldg_qty", "ovrs_stck_hold_qty");
            BigDecimal avgPrice = pathDecimal(item, "pchs_avg_pric", "ovrs_avg_pric", "avg_pric");
            BigDecimal currentPrice = pathDecimal(item, "prpr", "now_pric", "ovrs_stck_prpr");
            BigDecimal evalAmt = pathDecimal(item, "evlu_amt", "ovrs_stck_evlu_amt", "evlu_amt");
            BigDecimal profitLoss = pathDecimal(item, "evlu_pfls_amt", "ovrs_pfls_amt");
            BigDecimal profitLossRate = pathDecimal(item, "evlu_pfls_rt", "ovrs_pfls_rt");
            if (symbol == null || symbol.isEmpty()) {
                return null;
            }
            if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                currentPrice = avgPrice != null ? avgPrice : BigDecimal.ZERO;
            }
            if (evalAmt == null) {
                evalAmt = currentPrice.multiply(BigDecimal.valueOf(quantity));
            }
            if (profitLoss == null) {
                profitLoss = BigDecimal.ZERO;
            }
            if (profitLossRate == null) {
                profitLossRate = BigDecimal.ZERO;
            }
            if (avgPrice == null) {
                avgPrice = currentPrice;
            }
            return AccountPositionDto.builder()
                    .symbol(symbol)
                    .name(name != null ? name : symbol)
                    .quantity(quantity)
                    .averagePrice(avgPrice)
                    .currentPrice(currentPrice)
                    .totalValue(evalAmt)
                    .profitLoss(profitLoss)
                    .profitLossRate(profitLossRate)
                    .currency("USD")
                    .market("US")
                    .lastUpdated(LocalDateTime.now())
                    .purchaseAmount(avgPrice.multiply(BigDecimal.valueOf(quantity)))
                    .evaluationAmount(evalAmt)
                    .evaluationProfitLoss(profitLoss)
                    .evaluationProfitLossRate(profitLossRate)
                    .holdingQuantity(quantity)
                    .build();
        } catch (Exception e) {
            log.debug("해외 보유 종목 1건 파싱 스킵: {}", e.getMessage());
            return null;
        }
    }

    private static String pathText(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                String v = node.path(key).asText(null);
                if (v != null && !v.isEmpty()) {
                    return v;
                }
            }
        }
        return null;
    }

    private static int pathInt(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                return node.path(key).asInt(0);
            }
        }
        return 0;
    }

    private static BigDecimal pathDecimal(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.has(key)) {
                String s = node.path(key).asText(null);
                if (s != null && !s.isEmpty()) {
                    try {
                        return new BigDecimal(s);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * 회원가입 전 계좌인증용: API Key/Secret·서버타입·계좌번호로 주식잔고조회를 호출하여 계좌 유효 여부 확인.
     * (키·계좌번호는 로그에 남기지 않음)
     *
     * @param appKey      App Key (평문)
     * @param appSecret   App Secret (평문)
     * @param serverType  "1": 모의투자, "0": 실거래
     * @param accountNo   계좌번호 (형식: 12345678-12)
     * @param accessToken 이미 발급된 Access Token
     */
    public void verifyAccountByCredentials(String appKey, String appSecret, String serverType, String accountNo,
            String accessToken) {
        if (serverType == null || (!serverType.equals("0") && !serverType.equals("1"))) {
            throw new IllegalArgumentException("서버 타입이 올바르지 않습니다 (0: 실거래, 1: 모의투자)");
        }
        String baseUrl = getBaseUrl(serverType);
        String trId = getBalanceTrId(serverType);
        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, trId);
        Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                accountNo != null ? accountNo : "",
                Map.of(
                        "AFHR_FLPR_YN", "N",
                        "OFL_YN", "",
                        "INQR_DVSN", "01", // 01: 대출일별 (02: 종목별는 2026-02-11 공지로 제한됨)
                        "UNPR_DVSN", "01",
                        "FUND_STTL_ICLD_YN", "N",
                        "FNCG_AMT_AUTO_RDPT_YN", "N",
                        "PRCS_DVSN", "01",
                        "CTX_AREA_FK100", "",
                        "CTX_AREA_NK100", ""));
        URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_BALANCE, queryParams);
        RateLimiter rateLimiter = getApiRateLimiter(serverType);
        rateLimiter.acquirePermission();
        String responseJson = webClient.get()
                .uri(uri)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(10))
                .block();
        if (responseJson == null) {
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 없습니다.");
        }
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(responseJson);
        } catch (Exception e) {
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답 파싱 실패.", e);
        }
        String rtCd = rootNode.path("rt_cd").asText();
        if (!"0".equals(rtCd)) {
            String msg1 = rootNode.path("msg1").asText("");
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "계좌 조회 실패. 서버 타입(모의/실거래)과 계좌번호가 일치하는지 확인하세요. " + msg1);
        }
    }

    /**
     * 매수가능조회
     */
    public BuyableAmountDto inquireBuyableAmount(String userId, String accountNo, String symbol, BigDecimal price) {
        log.debug("매수가능조회: userId={}, accountNo={}, symbol={}, price={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo), symbol, price);

        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId, userApiKey.getServerType());
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getBuyableTrId(serverType);

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 조회 파라미터 (GET query parameter로 전달)
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "PDNO", symbol,
                            "ORD_DVSN", "01", // 주문구분 (01: 시장가)
                            "ORD_UNPR", price.toPlainString(),
                            "CMA_EVLU_AMT_ICLD_YN", "N", // CMA평가금액포함여부
                            "OVRS_ICLD_YN", "N" // 해외포함여부
                    ));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_PSBL_ORDER, queryParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("매수가능조회", uri, headers, queryParams);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
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
            log.error("매수가능조회 실패: userId={}, accountNo={}, symbol={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), symbol, e);
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
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId, userApiKey.getServerType());
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getSellableTrId(serverType);

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 조회 파라미터 (GET query parameter로 전달)
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "PDNO", symbol,
                            "ORD_DVSN", "01" // 주문구분 (01: 지정가)
                    ));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_PSBL_ORDER2, queryParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("매도가능수량조회", uri, headers, queryParams);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
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
            log.error("매도가능수량조회 실패: userId={}, accountNo={}, symbol={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), symbol, e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "매도가능수량조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주문체결조회
     */
    public List<OrderHistoryDto> inquireOrderHistory(String userId, String accountNo, LocalDate startDate,
            LocalDate endDate) {
        log.debug("주문체결조회: userId={}, accountNo={}, startDate={}, endDate={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo), startDate, endDate);

        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String accessToken = tokenService.getAccessToken(userId, userApiKey.getServerType());
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String serverType = userApiKey.getServerType();

            String baseUrl = getBaseUrl(serverType);
            String trId = getOrderHistoryTrId(serverType);

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 조회 파라미터 (GET query parameter로 전달)
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
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo, additionalParams);
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_DAILY_CCLD, queryParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("주문체결조회", uri, headers, queryParams);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
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
            log.error("주문체결조회 실패: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "주문체결조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 주식정정취소가능주문조회 (미체결 주문 목록). GET. TR_ID TTTC8002R/VTTC8002R.
     */
    public List<CancelableOrderDto> inquireCancelableOrders(String userId, String accountNo) {
        log.debug("주식정정취소가능주문조회: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo));
        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));
            String serverType = userApiKey.getServerType();
            String accessToken = tokenService.getAccessToken(userId, serverType);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String baseUrl = getBaseUrl(serverType);
            String trId = getCancelableOrderTrId(serverType);
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(accountNo, Map.of());
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_PSBL_RVSECNCL, queryParams);
            logApiRequest("주식정정취소가능주문조회", uri, headers, queryParams);
            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();
            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }
            logApiResponse("주식정정취소가능주문조회", responseJson);
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }
            JsonNode output1 = rootNode.path("output1");
            List<CancelableOrderDto> list = new ArrayList<>();
            if (output1.isArray()) {
                for (JsonNode item : output1) {
                    CancelableOrderDto dto = CancelableOrderDto.builder()
                            .accountNo(accountNo)
                            .symbol(item.path("pdno").asText(""))
                            .orderNo(item.path("odno").asText(""))
                            .orderBranchNo(item.path("ord_gno_brno").asText(""))
                            .orderType("01".equals(item.path("sll_buy_dvsn_cd").asText("")) ? "SELL" : "BUY")
                            .orderQuantity(Integer.parseInt(item.path("ord_qty").asText("0")))
                            .orderPrice(new BigDecimal(item.path("ord_unpr").asText("0")))
                            .orderTime(parseDateTime(item.path("ord_tmd").asText(""), item.path("ord_dt").asText("")))
                            .currency("KRW")
                            .build();
                    list.add(dto);
                }
            }
            return list;
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("주식정정취소가능주문조회 실패: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "주식정정취소가능주문조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 투자계좌자산현황조회
     */
    public AccountAssetDto inquireAssets(String userId, String accountNo) {
        log.debug("투자계좌자산현황조회: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo));

        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String serverType = userApiKey.getServerType();
            if ("1".equals(serverType)) {
                return inquireAssetsFallbackFromBalance(userId, accountNo);
            }
            String accessToken = tokenService.getAccessToken(userId, serverType);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

            String baseUrl = getBaseUrl(serverType);
            String trId = getAssetsTrId(serverType);

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 조회 파라미터 (GET query, 공식 스펙: CANO, ACNT_PRDT_CD, INQR_DVSN_1, BSPR_BF_DT_APLY_YN)
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "INQR_DVSN_1", "",
                            "BSPR_BF_DT_APLY_YN", ""));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_ACCOUNT_BALANCE, queryParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("투자계좌자산현황조회", uri, headers, queryParams);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
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

            // 투자계좌자산현황조회(inquire-account-balance) 응답: output2 단일 객체 (공식 예제 output2 필드명)
            JsonNode output2 = rootNode.path("output2");
            return AccountAssetDto.builder()
                    .accountNo(accountNo)
                    .totalAssetValue(new BigDecimal(output2.path("tot_asst_amt").asText("0")))
                    .deposit(new BigDecimal(output2.path("tot_dncl_amt").asText("0")))
                    .stockValue(new BigDecimal(output2.path("evlu_amt_smtl").asText("0")))
                    .totalProfitLoss(new BigDecimal(output2.path("evlu_pfls_amt_smtl").asText("0")))
                    .totalProfitLossRate(new BigDecimal(output2.path("evlu_pfls_rt").asText("0")))
                    .orderableCash(new BigDecimal(output2.path("dncl_amt").asText("0")))
                    .currency("KRW")
                    .build();

        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("투자계좌자산현황조회 실패: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "투자계좌자산현황조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 모의계좌용: 투자계좌자산현황조회 API 미지원이므로 주식잔고조회 결과로 자산 요약을 구성한다.
     */
    private AccountAssetDto inquireAssetsFallbackFromBalance(String userId, String accountNo) {
        log.debug("모의계좌: 투자계좌자산현황 API 미지원 → 주식잔고조회로 자산 요약 구성");
        BalanceAndPositionsResult result = inquireBalance(userId, accountNo);
        AccountBalanceDto balance = result.getBalance();
        BigDecimal total = balance.getTotalAssetValue() != null ? balance.getTotalAssetValue() : BigDecimal.ZERO;
        BigDecimal deposit = balance.getDeposit() != null ? balance.getDeposit() : BigDecimal.ZERO;
        BigDecimal stockValue = total.subtract(deposit).max(BigDecimal.ZERO);
        return AccountAssetDto.builder()
                .accountNo(accountNo)
                .totalAssetValue(total)
                .deposit(deposit)
                .stockValue(stockValue)
                .totalProfitLoss(balance.getTotalProfitLoss() != null ? balance.getTotalProfitLoss() : BigDecimal.ZERO)
                .totalProfitLossRate(balance.getTotalProfitLossRate() != null ? balance.getTotalProfitLossRate() : BigDecimal.ZERO)
                .orderableCash(balance.getOrderableCash() != null ? balance.getOrderableCash() : BigDecimal.ZERO)
                .currency("KRW")
                .build();
    }

    /**
     * 주식잔고조회_실현손익 (TTTC8494R/VTTC8494R). GET + query parameter.
     */
    public BalanceRealizedProfitLossDto inquireBalanceRealizedProfitLoss(String userId, String accountNo) {
        log.debug("주식잔고조회_실현손익: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo));
        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));
            String serverType = userApiKey.getServerType();
            String accessToken = tokenService.getAccessToken(userId, serverType);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String baseUrl = getBaseUrl(serverType);
            String trId = getBalanceRlzPlTrId(serverType);
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo != null ? accountNo : "",
                    Map.of(
                            "AFHR_FLPR_YN", "N",
                            "OFL_YN", "",
                            "INQR_DVSN", "00",
                            "UNPR_DVSN", "01",
                            "FUND_STTL_ICLD_YN", "N",
                            "FNCG_AMT_AUTO_RDPT_YN", "N",
                            "PRCS_DVSN", "01",
                            "COST_ICLD_YN", "",
                            "CTX_AREA_FK100", "",
                            "CTX_AREA_NK100", ""));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_BALANCE_RLZ_PL, queryParams);
            logApiRequest("주식잔고조회_실현손익", uri, headers, queryParams);
            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();
            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }
            logApiResponse("주식잔고조회_실현손익", responseJson);
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }
            JsonNode output2 = rootNode.path("output2");
            BigDecimal totalRlzPl = BigDecimal.ZERO;
            if (output2 != null && !output2.isNull()) {
                JsonNode o2 = output2.isArray() && output2.size() > 0 ? output2.get(0) : output2;
                if (o2.isObject()) {
                    totalRlzPl = pathDecimal(o2, "tot_rlzt_pfls", "tot_rlz_pfls");
                    if (totalRlzPl == null) {
                        totalRlzPl = BigDecimal.ZERO;
                    }
                }
            }
            List<BalanceRealizedProfitLossDto.RealizedProfitLossItemDto> details = new ArrayList<>();
            JsonNode output1 = rootNode.path("output1");
            if (output1 != null && output1.isArray()) {
                for (JsonNode item : output1) {
                    String tradeDate = item.has("trad_dt") ? item.path("trad_dt").asText("") : "";
                    BigDecimal rlzt = pathDecimal(item, "rlzt_pfls", "rlz_pfls");
                    if (rlzt == null) {
                        rlzt = BigDecimal.ZERO;
                    }
                    BigDecimal buyAmt = pathDecimal(item, "buy_amt");
                    if (buyAmt == null) {
                        buyAmt = BigDecimal.ZERO;
                    }
                    BigDecimal sllAmt = pathDecimal(item, "sll_amt");
                    if (sllAmt == null) {
                        sllAmt = BigDecimal.ZERO;
                    }
                    details.add(BalanceRealizedProfitLossDto.RealizedProfitLossItemDto.builder()
                            .tradeDate(tradeDate)
                            .realizedProfitLoss(rlzt)
                            .buyAmount(buyAmt)
                            .sellAmount(sllAmt)
                            .build());
                }
            }
            return BalanceRealizedProfitLossDto.builder()
                    .accountNo(accountNo != null ? accountNo : "")
                    .totalRealizedProfitLoss(totalRlzPl)
                    .currency("KRW")
                    .details(details)
                    .build();
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("주식잔고조회_실현손익 실패: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "주식잔고조회_실현손익 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기간별손익조회
     */
    public ProfitLossDto inquirePeriodProfitLoss(String userId, String accountNo, LocalDate startDate,
            LocalDate endDate) {
        log.debug("기간별손익조회: userId={}, accountNo={}, startDate={}, endDate={}", LogMaskingUtil.maskUserId(userId),
                LogMaskingUtil.maskAccountNo(accountNo), startDate, endDate);

        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            String serverType = userApiKey.getServerType();
            // 명세 10-korea-investment-api-spec §6: 기간별손익일별합산조회는 모의투자 미지원. 호출 시 404 발생하므로 미호출.
            if ("1".equals(serverType)) {
                throw new DomainException(ErrorCode.API_NOT_SUPPORTED,
                        "기간별손익조회는 모의투자 환경에서 미지원됩니다. 실계좌에서만 이용 가능합니다.");
            }

            String accessToken = tokenService.getAccessToken(userId, userApiKey.getServerType());
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());

            String baseUrl = getBaseUrl(serverType);
            String trId = getPeriodProfitLossTrId(serverType);

            // 요청 헤더 생성 (공통 유틸리티 사용)
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);

            // 조회 파라미터 (GET query parameter). 명세 10-korea-investment-api-spec §6: PDNO, SORT_DVSN, CBLC_DVSN 필수.
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo,
                    Map.of(
                            "INQR_STRT_DT", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            "INQR_END_DT", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            "PDNO", "", // 공란: 전체 종목
                            "SORT_DVSN", "00", // 00: 최근순
                            "INQR_DVSN", "00",
                            "CBLC_DVSN", "00", // 00: 전체 잔고
                            "CTX_AREA_FK100", "",
                            "CTX_AREA_NK100", ""));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_PERIOD_PROFIT_LOSS, queryParams);

            // local 환경에서 요청 상세 로그 출력
            logApiRequest("기간별손익조회", uri, headers, queryParams);

            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();

            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
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
            log.error("기간별손익조회 실패: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "기간별손익조회 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 기간별매매손익현황조회 (TTTC8709R/VTTC8709R). GET + query parameter.
     */
    public PeriodProfitLossStatusDto inquirePeriodProfitLossStatus(String userId, String accountNo,
            LocalDate startDate, LocalDate endDate) {
        log.debug("기간별매매손익현황조회: userId={}, accountNo={}, startDate={}, endDate={}",
                LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), startDate, endDate);
        try {
            UserApiKey userApiKey = getUserApiKeyForAccount(userId, accountNo)
                    .orElseThrow(() -> new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                            "한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));
            String serverType = userApiKey.getServerType();
            String accessToken = tokenService.getAccessToken(userId, serverType);
            String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
            String baseUrl = getBaseUrl(serverType);
            String trId = getPeriodProfitLossStatusTrId(serverType);
            HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                    accessToken, appKey, appSecret, trId);
            Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                    accountNo != null ? accountNo : "",
                    Map.of(
                            "SORT_DVSN", "00",
                            "INQR_STRT_DT", startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            "INQR_END_DT", endDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                            "CBLC_DVSN", "00",
                            "PDNO", "",
                            "CTX_AREA_FK100", "",
                            "CTX_AREA_NK100", ""));
            URI uri = buildUriWithQueryParams(baseUrl, PATH_INQUIRE_PERIOD_PROFIT_LOSS_STATUS, queryParams);
            logApiRequest("기간별매매손익현황조회", uri, headers, queryParams);
            RateLimiter rateLimiter = getApiRateLimiter(serverType);
            rateLimiter.acquirePermission();
            String responseJson = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            if (responseJson == null) {
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 응답이 null입니다");
            }
            logApiResponse("기간별매매손익현황조회", responseJson);
            JsonNode rootNode = objectMapper.readTree(responseJson);
            String rtCd = rootNode.path("rt_cd").asText();
            if (!"0".equals(rtCd)) {
                String msg1 = rootNode.path("msg1").asText();
                throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
            }
            BigDecimal totalRlzPl = BigDecimal.ZERO;
            JsonNode output2 = rootNode.path("output2");
            if (output2 != null && !output2.isNull()) {
                JsonNode o2 = output2.isArray() && output2.size() > 0 ? output2.get(0) : output2;
                if (o2.isObject()) {
                    totalRlzPl = pathDecimal(o2, "tot_rlzt_pfls", "tot_rlz_pfls", "rlz_pfls_amt");
                    if (totalRlzPl == null) {
                        totalRlzPl = BigDecimal.ZERO;
                    }
                }
            }
            List<PeriodProfitLossStatusDto.ProfitLossStatusItemDto> items = new ArrayList<>();
            JsonNode output1 = rootNode.path("output1");
            if (output1 != null && output1.isArray()) {
                for (JsonNode item : output1) {
                    String symbol = pathText(item, "pdno", "종목코드");
                    String name = pathText(item, "prdt_name", "종목명");
                    BigDecimal rlzt = pathDecimal(item, "rlzt_pfls", "rlz_pfls", "evlu_pfls_amt");
                    if (rlzt == null) {
                        rlzt = BigDecimal.ZERO;
                    }
                    BigDecimal buyAmt = pathDecimal(item, "buy_amt", "pchs_amt");
                    if (buyAmt == null) {
                        buyAmt = BigDecimal.ZERO;
                    }
                    BigDecimal sllAmt = pathDecimal(item, "sll_amt", "sll_amt_smtl");
                    if (sllAmt == null) {
                        sllAmt = BigDecimal.ZERO;
                    }
                    items.add(PeriodProfitLossStatusDto.ProfitLossStatusItemDto.builder()
                            .symbol(symbol != null ? symbol : "")
                            .symbolName(name != null ? name : "")
                            .realizedProfitLoss(rlzt)
                            .buyAmount(buyAmt)
                            .sellAmount(sllAmt)
                            .build());
                }
            }
            return PeriodProfitLossStatusDto.builder()
                    .accountNo(accountNo != null ? accountNo : "")
                    .startDate(startDate)
                    .endDate(endDate)
                    .totalRealizedProfitLoss(totalRlzPl)
                    .currency("KRW")
                    .items(items)
                    .build();
        } catch (DomainException e) {
            throw e;
        } catch (Exception e) {
            log.error("기간별매매손익현황조회 실패: userId={}, accountNo={}", LogMaskingUtil.maskUserId(userId),
                    LogMaskingUtil.maskAccountNo(accountNo), e);
            throw new DomainException(ErrorCode.ACCOUNT_NOT_FOUND,
                    "기간별매매손익현황조회 실패: " + e.getMessage(), e);
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

        // 모의계좌 등에서 tot_evlu_amt가 0으로 올 때: 예수금+주문가능금액으로 총자산 표시 (한투 모의 100만원 등)
        if (totalAssetValue.compareTo(BigDecimal.ZERO) == 0
                && (deposit.compareTo(BigDecimal.ZERO) > 0 || orderableCash.compareTo(BigDecimal.ZERO) > 0)) {
            totalAssetValue = deposit.add(orderableCash);
        }

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

            // 거래소 구분: 국내 잔고 API는 KRX만 반환. 해외 포함 시 응답 필드(예: excg_dvsn_cd)로 US 구분 가능.
            String market = "KR";
            if (item.has("excg_dvsn_cd")) {
                String excg = item.path("excg_dvsn_cd").asText("");
                if ("NASD".equals(excg) || "NYSE".equals(excg) || "AMEX".equals(excg)) {
                    market = "US";
                }
            }
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
                    .market(market)
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

    /**
     * 해외(미국) 잔고 조회 결과: 보유 종목 목록 + output2 요약(예수금·총자산)
     */
    public static class OverseasBalanceResult {
        private final List<AccountPositionDto> positions;
        private final OverseasBalanceSummaryDto summary;

        public OverseasBalanceResult(List<AccountPositionDto> positions, OverseasBalanceSummaryDto summary) {
            this.positions = positions != null ? positions : new ArrayList<>();
            this.summary = summary;
        }

        public List<AccountPositionDto> getPositions() {
            return positions;
        }

        public OverseasBalanceSummaryDto getSummary() {
            return summary;
        }
    }
}
