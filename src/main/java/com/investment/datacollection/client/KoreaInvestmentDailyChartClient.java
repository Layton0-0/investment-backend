package com.investment.datacollection.client;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.KoreaInvestmentRequestBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * KRX 일봉 수집 실패 시 한투 API 보조 소스용: 특정 종목·특정 일자의 일봉 1건 조회.
 * 주식현재가 일봉차트 조회(inquire-daily-itemchartprice) 사용, FID_ORG_ADJ_PRC=0(수정주가).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KoreaInvestmentDailyChartClient {

    private static final String MARKET_KR = "KR";
    private static final String TR_ID_DAILY_CHART = "FHKST03010100";
    private static final String PATH_DAILY_CHART = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final WebClient webClient;
    private final MarketDataProperties marketDataProperties;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * 한투 API로 해당 일자 해당 종목 일봉 1건 조회 (수정주가).
     *
     * @param symbol 6자리 종목코드
     * @param basDt  기준일
     * @param userId API 키 소유 사용자 ID (토큰 발급용)
     * @return 해당 일자 일봉 1건, 없거나 실패 시 empty
     */
    public Optional<DailyStock> fetchDailyForSymbol(String symbol, LocalDate basDt, String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        Optional<Map<String, String>> tokenInfoOpt = buildTokenInfo(userId);
        if (tokenInfoOpt.isEmpty()) {
            log.warn("KRX 폴백: 한투 토큰/API키 없음, userId={}", userId);
            return Optional.empty();
        }
        Map<String, String> tokenInfo = tokenInfoOpt.get();

        String baseUrl = "1".equals(tokenInfo.get("serverType"))
                ? "https://openapivts.koreainvestment.com:29443"
                : "https://openapi.koreainvestment.com:9443";
        String accessToken = tokenInfo.get("token");
        String appKey = tokenInfo.get("appKey");
        String appSecret = tokenInfo.get("appSecret");

        String dateStr = basDt.format(YMD);
        Map<String, String> queryParams = KoreaInvestmentRequestBuilder.createMarketDataRequestBody(
                Map.of(
                        "FID_COND_MRKT_DIV_CODE", "J",
                        "FID_INPUT_ISCD", symbol,
                        "FID_INPUT_DATE_1", dateStr,
                        "FID_INPUT_DATE_2", dateStr,
                        "FID_PERIOD_DIV_CODE", "D",
                        "FID_ORG_ADJ_PRC", "0"
                ));
        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl).path(PATH_DAILY_CHART)
                .queryParam("FID_COND_MRKT_DIV_CODE", queryParams.get("FID_COND_MRKT_DIV_CODE"))
                .queryParam("FID_INPUT_ISCD", queryParams.get("FID_INPUT_ISCD"))
                .queryParam("FID_INPUT_DATE_1", queryParams.get("FID_INPUT_DATE_1"))
                .queryParam("FID_INPUT_DATE_2", queryParams.get("FID_INPUT_DATE_2"))
                .queryParam("FID_PERIOD_DIV_CODE", queryParams.get("FID_PERIOD_DIV_CODE"))
                .queryParam("FID_ORG_ADJ_PRC", queryParams.get("FID_ORG_ADJ_PRC"))
                .build().toUri();

        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(
                accessToken, appKey, appSecret, TR_ID_DAILY_CHART);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = webClient.get()
                    .uri(uri)
                    .headers(h -> h.addAll(headers))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(java.time.Duration.ofMillis(marketDataProperties.getTimeout()))
                    .block();

            if (res == null || !"0".equals(res.get("rt_cd"))) {
                log.debug("한투 일봉 조회 실패 또는 빈 응답: symbol={}, basDt={}, rt_cd={}", symbol, basDt, res != null ? res.get("rt_cd") : null);
                return Optional.empty();
            }

            Object output2 = res.get("output2");
            if (!(output2 instanceof List) || ((List<?>) output2).isEmpty()) {
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) ((List<?>) output2).get(0);
            return Optional.of(mapToDailyStock(row, symbol, basDt));
        } catch (Exception e) {
            log.debug("한투 일봉 조회 예외: symbol={}, basDt={}, error={}", symbol, basDt, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<Map<String, String>> buildTokenInfo(String userId) {
        var userApiKeyOpt = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT);
        if (userApiKeyOpt.isEmpty()) return Optional.empty();
        var userApiKey = userApiKeyOpt.get();
        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";
        String accessToken;
        try {
            accessToken = tokenService.getAccessToken(userId, serverType);
        } catch (Exception e) {
            log.debug("한투 토큰 조회 실패: userId={}, error={}", userId, e.getMessage());
            return Optional.empty();
        }
        if (accessToken == null || accessToken.isBlank()) return Optional.empty();
        String appKey;
        String appSecret;
        try {
            appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
        } catch (RuntimeException e) {
            log.warn("한투 API키 복호화 실패: userId={}", userId);
            return Optional.empty();
        }
        return Optional.of(Map.of(
                "token", accessToken,
                "serverType", serverType,
                "appKey", appKey,
                "appSecret", appSecret
        ));
    }

    private static DailyStock mapToDailyStock(Map<String, Object> row, String symbol, LocalDate basDt) {
        BigDecimal openPrice = toBigDecimal(row.get("stck_oprc"));
        BigDecimal highPrice = toBigDecimal(row.get("stck_hgpr"));
        BigDecimal lowPrice = toBigDecimal(row.get("stck_lwpr"));
        BigDecimal closePrice = toBigDecimal(row.get("stck_clpr"));
        Long volume = toLong(row.get("acml_vol"));
        Long trdVal = toLong(row.get("acml_vol_amt"));

        return DailyStock.builder()
                .basDt(basDt)
                .symbol(symbol)
                .market(MARKET_KR)
                .openPrice(openPrice)
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .closePrice(closePrice)
                .volume(volume)
                .trdVal(trdVal != null ? trdVal : (volume != null && closePrice != null ? volume * closePrice.longValue() : null))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static BigDecimal toBigDecimal(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try {
            return new BigDecimal(v.toString().replaceAll(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long toLong(Object v) {
        if (v == null || v.toString().isBlank()) return null;
        try {
            return Long.parseLong(v.toString().replaceAll(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
