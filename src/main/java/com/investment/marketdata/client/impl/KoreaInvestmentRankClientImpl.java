package com.investment.marketdata.client.impl;

import com.investment.common.logging.KoreaInvestmentApiLogging;
import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.client.KoreaInvestmentRankClient;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.dto.InvestorDailyByMarketItemDto;
import com.investment.marketdata.dto.VolumeRankItemDto;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.KoreaInvestmentRequestBuilder;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 한국투자증권 순위분석·투자자 매매동향 API 구현체.
 * path·TR_ID는 application.yml rankApi 또는 MCP로 확인 후 설정.
 * 미설정 시 해당 메서드는 빈 리스트 반환.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "investment.market-data.provider", havingValue = "korea-investment")
public class KoreaInvestmentRankClientImpl implements KoreaInvestmentRankClient {

    private static final String BASE_URL_REAL = "https://openapi.koreainvestment.com:9443";
    private static final String BASE_URL_VIRTUAL = "https://openapivts.koreainvestment.com:29443";
    private static final String RATE_LIMITER_API_VIRTUAL = "koreaInvestmentApi";
    private static final String RATE_LIMITER_API_REAL = "koreaInvestmentApiReal";

    private final MarketDataProperties properties;
    private final WebClient webClient;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final KoreaInvestmentTokenService tokenService;
    private final UserApiKeyRepository userApiKeyRepository;
    private final EncryptionUtil encryptionUtil;

    @Override
    public List<VolumeRankItemDto> getVolumeRank(String userId, String serverType, String marketDiv, int limit) {
        MarketDataProperties.KoreaInvestmentProperties.RankApiProperties rank = properties.getKoreaInvestment().getRankApi();
        if (rank.getVolumeRankPath() == null || rank.getVolumeRankPath().isBlank()
                || rank.getVolumeRankTrId() == null || rank.getVolumeRankTrId().isBlank()) {
            log.debug("순위 API 미설정(volumeRankPath/volumeRankTrId). MCP로 확인 후 설정. 빈 리스트 반환.");
            return List.of();
        }

        return getTokenAndCall(userId, serverType, rank.getVolumeRankPath(), rank.getVolumeRankTrId(),
                KoreaInvestmentRequestBuilder.createMarketDataRequestBody(Map.of(
                        "FID_COND_MRKT_DIV_CODE", marketDiv != null ? marketDiv : "J",
                        "FID_DATA_CNT", String.valueOf(Math.min(limit, 100))
                )), "거래량순위")
                .map(this::parseVolumeRankOutput)
                .blockOptional(Duration.ofMillis(properties.getTimeout() + 5000))
                .orElse(List.of());
    }

    @Override
    public List<InvestorDailyByMarketItemDto> getInvestorDailyByMarket(String userId, String serverType,
                                                                        LocalDate fromDate, LocalDate toDate) {
        MarketDataProperties.KoreaInvestmentProperties.RankApiProperties rank = properties.getKoreaInvestment().getRankApi();
        if (rank.getInvestorDailyPath() == null || rank.getInvestorDailyPath().isBlank()
                || rank.getInvestorDailyTrId() == null || rank.getInvestorDailyTrId().isBlank()) {
            log.debug("투자자 매매동향 API 미설정(investorDailyPath/investorDailyTrId). MCP로 확인 후 설정. 빈 리스트 반환.");
            return List.of();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");
        Map<String, String> params = new java.util.HashMap<>(Map.of(
                "FID_STT_DATE", fromDate.format(fmt),
                "FID_END_DATE", toDate.format(fmt)
        ));

        return getTokenAndCall(userId, serverType, rank.getInvestorDailyPath(), rank.getInvestorDailyTrId(),
                KoreaInvestmentRequestBuilder.createMarketDataRequestBody(params), "투자자매매동향")
                .map(this::parseInvestorDailyOutput)
                .blockOptional(Duration.ofMillis(properties.getTimeout() + 5000))
                .orElse(List.of());
    }

    private Mono<Map<String, Object>> getTokenAndCall(String userId, String serverType, String path, String trId,
                                                      Map<String, String> queryParams, String apiName) {
        String st = serverType != null ? serverType : "1";
        Optional<UserApiKey> keyOpt = userApiKeyRepository.findByUserIdAndBrokerTypeAndServerType(
                userId, BrokerType.KOREA_INVESTMENT, st);
        if (keyOpt.isEmpty()) {
            return Mono.error(new IllegalStateException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId + ", serverType=" + st));
        }
        UserApiKey userApiKey = keyOpt.get();
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
        String accessToken = tokenService.getAccessToken(userId, st);

        String baseUrl = "1".equals(st) ? BASE_URL_VIRTUAL : BASE_URL_REAL;
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
        if (queryParams != null) {
            queryParams.forEach(builder::queryParam);
        }
        URI uri = builder.build().toUri();

        KoreaInvestmentApiLogging.logRequest(apiName, path, trId, queryParams != null ? queryParams.keySet() : null);
        KoreaInvestmentApiLogging.logApiCallInfo("한국투자증권", apiName, uri.toString(), "GET");

        HttpHeaders headers = KoreaInvestmentRequestBuilder.createCommonHeaders(accessToken, appKey, appSecret, trId);
        RateLimiter rateLimiter = "0".equals(st)
                ? rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_REAL)
                : rateLimiterRegistry.rateLimiter(RATE_LIMITER_API_VIRTUAL);

        return Mono.fromCallable(() -> {
            rateLimiter.acquirePermission();
            return null;
        }).flatMap(ignored -> webClient.get()
                .uri(uri)
                .headers(h -> h.addAll(headers))
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)).filter(t -> t instanceof org.springframework.web.reactive.function.client.WebClientResponseException w
                        && w.getStatusCode().is5xxServerError()))
                .map(m -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) m;
                    String rtCd = (String) map.get("rt_cd");
                    String msgCd = (String) map.get("msg_cd");
                    String msg1 = (String) map.get("msg1");
                    if (rtCd == null || !"0".equals(rtCd)) {
                        KoreaInvestmentApiLogging.logResponseError(apiName, 200, rtCd, msgCd, msg1, null);
                        throw new RuntimeException("한국투자증권 API 오류: rt_cd=" + rtCd + ", msg1=" + msg1);
                    }
                    KoreaInvestmentApiLogging.logResponseSuccessFromMap(apiName, 200, map);
                    return map;
                })
                .doOnError(e -> KoreaInvestmentApiLogging.logFailure(apiName, e)));
    }

    @SuppressWarnings("unchecked")
    private List<VolumeRankItemDto> parseVolumeRankOutput(Map<String, Object> response) {
        List<VolumeRankItemDto> list = new ArrayList<>();
        Object output2 = response.get("output2");
        if (!(output2 instanceof List)) {
            return list;
        }
        for (Object row : (List<?>) output2) {
            if (!(row instanceof Map)) continue;
            Map<String, Object> m = (Map<String, Object>) row;
            list.add(VolumeRankItemDto.builder()
                    .symbol((String) m.get("mksc_shrn_iscd"))
                    .name((String) m.get("hts_kor_isnm"))
                    .volume(toLong(m.get("acml_vol")))
                    .amount(toBigDecimal(m.get("acml_vol_amt")))
                    .marketDiv((String) m.get("mkt_div"))
                    .rank(toInt(m.get("data_rank")))
                    .build());
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<InvestorDailyByMarketItemDto> parseInvestorDailyOutput(Map<String, Object> response) {
        List<InvestorDailyByMarketItemDto> list = new ArrayList<>();
        Object output = response.get("output");
        if (output instanceof Map) {
            Map<String, Object> m = (Map<String, Object>) output;
            String dateStr = (String) m.get("stck_bsop_date");
            list.add(InvestorDailyByMarketItemDto.builder()
                    .stckBsopDate(dateStr != null && dateStr.length() >= 8 ? LocalDate.parse(dateStr.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE) : null)
                    .prsnNtbyQty((String) m.get("prsn_ntby_qty"))
                    .frgnNtbyQty((String) m.get("frgn_ntby_qty"))
                    .orgnNtbyQty((String) m.get("orgn_ntby_qty"))
                    .prsnNtbyAmt(toBigDecimal(m.get("prsn_ntby_amt")))
                    .frgnNtbyAmt(toBigDecimal(m.get("frgn_ntby_amt")))
                    .orgnNtbyAmt(toBigDecimal(m.get("orgn_ntby_amt")))
                    .build());
        } else if (output instanceof List) {
            for (Object row : (List<?>) output) {
                if (!(row instanceof Map)) continue;
                Map<String, Object> m = (Map<String, Object>) row;
                String dateStr = (String) m.get("stck_bsop_date");
                list.add(InvestorDailyByMarketItemDto.builder()
                        .stckBsopDate(dateStr != null && dateStr.length() >= 8 ? LocalDate.parse(dateStr.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE) : null)
                        .prsnNtbyQty((String) m.get("prsn_ntby_qty"))
                        .frgnNtbyQty((String) m.get("frgn_ntby_qty"))
                        .orgnNtbyQty((String) m.get("orgn_ntby_qty"))
                        .prsnNtbyAmt(toBigDecimal(m.get("prsn_ntby_amt")))
                        .frgnNtbyAmt(toBigDecimal(m.get("frgn_ntby_amt")))
                        .orgnNtbyAmt(toBigDecimal(m.get("orgn_ntby_amt")))
                        .build());
            }
        }
        return list;
    }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(o.toString().replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(o.toString().replace(",", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
