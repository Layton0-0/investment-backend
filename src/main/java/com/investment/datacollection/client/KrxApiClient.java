package com.investment.datacollection.client;

import com.investment.common.logging.KoreaInvestmentApiLogging;
import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.dto.KrxDailyStockResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * KRX Open API 클라이언트 (일별 시세).
 * <p>명세: docs/04-api/12-krx-api-spec/ (01-stk-bydd-trd, 06-ksq-bydd-trd).
 * 공통: GET ?basDd=yyyyMMdd, 헤더 AUTH_KEY.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxApiClient {

    private static final DateTimeFormatter KRX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 유가증권 일별매매정보 (01-stk-bydd-trd) */
    private static final String PATH_STK_BYDD_TRD = "/svc/apis/sto/stk_bydd_trd";
    /** 코스닥 일별매매정보 (06-ksq-bydd-trd). 동일 OutBlock_1 구조 */
    private static final String PATH_KSQ_BYDD_TRD = "/svc/apis/sto/ksq_bydd_trd";

    private final RestTemplate restTemplate = new RestTemplate();
    private final DataCollectionProperties dataCollectionProperties;

    /**
     * 유가증권 일별매매정보 조회 (기준일자).
     * 명세: 12-krx-api-spec/01-stk-bydd-trd.md
     *
     * @param basDt 기준일자 (yyyyMMdd)
     * @return OutBlock_1 리스트. 인증키 미설정 또는 실패 시 빈 리스트
     */
    public List<Map<String, Object>> fetchDailyStockKospi(LocalDate basDt) {
        return fetchDailyStock(basDt, PATH_STK_BYDD_TRD, "유가증권 일별매매정보(stk_bydd_trd)");
    }

    /**
     * 코스닥 일별매매정보 조회 (기준일자).
     * 명세: 12-krx-api-spec/06-ksq-bydd-trd.md (OutBlock_1 구조 동일)
     *
     * @param basDt 기준일자 (yyyyMMdd)
     * @return OutBlock_1 리스트. 인증키 미설정 또는 실패 시 빈 리스트
     */
    public List<Map<String, Object>> fetchDailyStockKosdaq(LocalDate basDt) {
        return fetchDailyStock(basDt, PATH_KSQ_BYDD_TRD, "코스닥 일별매매정보(ksq_bydd_trd)");
    }

    /**
     * KRX 일별매매 API 공통 호출. GET ?basDd=yyyyMMdd, 헤더 AUTH_KEY.
     */
    private List<Map<String, Object>> fetchDailyStock(LocalDate basDt, String path, String apiName) {
        String authKey = dataCollectionProperties.getKrx().getAuthKey();
        if (authKey == null || authKey.isBlank()) {
            log.warn("KRX AUTH_KEY 미설정: 조회 스킵");
            return Collections.emptyList();
        }

        String baseUrl = dataCollectionProperties.getKrx().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://data-dbg.krx.co.kr";
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + path)
                .queryParam("basDd", basDt.format(KRX_DATE))
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("AUTH_KEY", authKey);
        headers.set(HttpHeaders.ACCEPT, "application/json");

        KoreaInvestmentApiLogging.logApiCallInfo("KRX", apiName, url, "GET");

        try {
            ResponseEntity<KrxDailyStockResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KrxDailyStockResponseDto.class);
            KrxDailyStockResponseDto body = response.getBody();
            if (body == null || body.getOutBlock1() == null) {
                return Collections.emptyList();
            }
            return body.getOutBlock1();
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("text/html")) {
                log.warn("KRX API가 HTML을 반환했습니다. basDt={}, api={}. AUTH_KEY 유효성 및 서비스 이용신청 여부를 확인하세요. (원인: {})",
                        basDt.format(KRX_DATE), apiName, msg);
            } else {
                log.warn("KRX API 호출 실패: basDt={}, api={}, error={}", basDt.format(KRX_DATE), apiName, msg);
            }
            return Collections.emptyList();
        }
    }
}
