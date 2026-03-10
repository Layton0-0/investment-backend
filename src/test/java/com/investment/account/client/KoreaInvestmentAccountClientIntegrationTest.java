package com.investment.account.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.common.util.AccountNumberUtil;
import com.investment.marketdata.util.KoreaInvestmentRequestBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.Map;

/**
 * 한국투자증권 계좌 API 실제 호출 통합 테스트.
 * <p>
 * 환경 변수 설정 시에만 실행됩니다. 모의계좌 사용 권장.
 * <ul>
 *   <li>RUN_KOREA_INVESTMENT_INTEGRATION=true</li>
 *   <li>KOREA_INVESTMENT_TEST_APP_KEY=모의 App Key</li>
 *   <li>KOREA_INVESTMENT_TEST_APP_SECRET=모의 App Secret</li>
 *   <li>KOREA_INVESTMENT_TEST_ACCOUNT_NO=12345678-01 (8-2 형식)</li>
 *   <li>KOREA_INVESTMENT_TEST_SERVER_TYPE=1 (1: 모의, 0: 실거래)</li>
 * </ul>
 * 실행 예: {@code set RUN_KOREA_INVESTMENT_INTEGRATION=true; set KOREA_INVESTMENT_TEST_APP_KEY=...; ...; .\gradlew test --tests "*KoreaInvestmentAccountClientIntegrationTest*"}
 */
@DisplayName("한국투자증권 계좌 API 실제 호출 통합 테스트")
@EnabledIfEnvironmentVariable(named = "RUN_KOREA_INVESTMENT_INTEGRATION", matches = "true")
class KoreaInvestmentAccountClientIntegrationTest {

    private static final String ENV_APP_KEY = "KOREA_INVESTMENT_TEST_APP_KEY";
    private static final String ENV_APP_SECRET = "KOREA_INVESTMENT_TEST_APP_SECRET";
    private static final String ENV_ACCOUNT_NO = "KOREA_INVESTMENT_TEST_ACCOUNT_NO";
    private static final String ENV_SERVER_TYPE = "KOREA_INVESTMENT_TEST_SERVER_TYPE";

    private WebClient webClient;
    private ObjectMapper objectMapper;
    private String appKey;
    private String appSecret;
    private String accountNo;
    private String serverType;

    @BeforeEach
    void setUp() {
        webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
        objectMapper = new ObjectMapper();

        appKey = System.getenv(ENV_APP_KEY);
        appSecret = System.getenv(ENV_APP_SECRET);
        accountNo = System.getenv(ENV_ACCOUNT_NO);
        serverType = System.getenv(ENV_SERVER_TYPE);
        if (serverType == null || serverType.isBlank()) {
            serverType = "1";
        }

        Assumptions.assumeTrue(appKey != null && !appKey.isBlank(),
                ENV_APP_KEY + " 설정 필요");
        Assumptions.assumeTrue(appSecret != null && !appSecret.isBlank(),
                ENV_APP_SECRET + " 설정 필요");
        Assumptions.assumeTrue(accountNo != null && !accountNo.isBlank(),
                ENV_ACCOUNT_NO + " 설정 필요 (형식: 8자리-2자리)");
        Assumptions.assumeTrue(AccountNumberUtil.validateAccountNumberFormat(accountNo.trim()),
                ENV_ACCOUNT_NO + " 형식이 올바르지 않음 (예: 12345678-01)");
    }

    @Test
    @DisplayName("접근토큰 발급 → 주식잔고조회 실제 API 호출 성공")
    void tokenAndInquireBalance_realApi_success() throws Exception {
        String baseUrl = "1".equals(serverType)
                ? KoreaInvestmentAccountApiConstants.BASE_URL_VIRTUAL
                : KoreaInvestmentAccountApiConstants.BASE_URL_REAL;

        // 1) 접근토큰 발급
        URI tokenUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/oauth2/tokenP")
                .build()
                .toUri();
        Map<String, String> tokenBody = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret);

        String tokenResponse = webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

        Assumptions.assumeTrue(tokenResponse != null && !tokenResponse.isBlank(),
                "토큰 발급 응답 없음");
        JsonNode tokenJson = objectMapper.readTree(tokenResponse);
        String accessToken = tokenJson.path("access_token").asText(null);
        Assumptions.assumeTrue(accessToken != null && !accessToken.isBlank(),
                "access_token 없음: " + tokenResponse);

        // 2) 주식잔고조회
        String trId = "1".equals(serverType)
                ? KoreaInvestmentAccountApiConstants.TR_ID_BALANCE_VIRTUAL
                : KoreaInvestmentAccountApiConstants.TR_ID_BALANCE_REAL;
        Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createAccountRequestBody(
                accountNo.trim(),
                Map.of(
                        "AFHR_FLPR_YN", "N",
                        "OFL_YN", "",
                        "INQR_DVSN", "01",
                        "UNPR_DVSN", "01",
                        "FUND_STTL_ICLD_YN", "N",
                        "FNCG_AMT_AUTO_RDPT_YN", "N",
                        "PRCS_DVSN", "01",
                        "CTX_AREA_FK100", "",
                        "CTX_AREA_NK100", ""));

        URI balanceUri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(KoreaInvestmentAccountApiConstants.PATH_INQUIRE_BALANCE)
                .queryParam("CANO", queryParams.get("CANO"))
                .queryParam("ACNT_PRDT_CD", queryParams.get("ACNT_PRDT_CD"))
                .queryParam("AFHR_FLPR_YN", "N")
                .queryParam("OFL_YN", "")
                .queryParam("INQR_DVSN", "01")
                .queryParam("UNPR_DVSN", "01")
                .queryParam("FUND_STTL_ICLD_YN", "N")
                .queryParam("FNCG_AMT_AUTO_RDPT_YN", "N")
                .queryParam("PRCS_DVSN", "01")
                .queryParam("CTX_AREA_FK100", "")
                .queryParam("CTX_AREA_NK100", "")
                .build()
                .toUri();

        String balanceResponse = webClient.get()
                .uri(balanceUri)
                .header("Authorization", "Bearer " + accessToken)
                .header("appkey", appKey)
                .header("appsecret", appSecret)
                .header("tr_id", trId)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(15))
                .block();

        Assumptions.assumeTrue(balanceResponse != null && !balanceResponse.isBlank(),
                "잔고조회 응답 없음");
        JsonNode balanceJson = objectMapper.readTree(balanceResponse);
        String rtCd = balanceJson.path("rt_cd").asText("");
        String msg1 = balanceJson.has("msg1") ? balanceJson.path("msg1").asText("") : "";

        if (!"0".equals(rtCd)) {
            throw new AssertionError("한국투자증권 주식잔고조회 실패: rt_cd=" + rtCd + ", msg1=" + msg1 + ", body=" + balanceResponse);
        }

        boolean hasOutput = balanceJson.has("output") && !balanceJson.path("output").isNull()
                || balanceJson.has("output2") && balanceJson.path("output2").isArray() && balanceJson.path("output2").size() > 0
                || balanceJson.has("output2") && balanceJson.path("output2").isObject();
        if (!hasOutput) {
            throw new AssertionError("주식잔고조회 응답에 output/output2 없음: " + balanceResponse);
        }
    }
}
