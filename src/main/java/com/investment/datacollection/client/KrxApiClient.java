package com.investment.datacollection.client;

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
 * KRX Open API 클라이언트 (일별 시세/지수 등)
 * AUTH_KEY 헤더로 인증. 1단계: 연동·조회만, 저장은 별도 테이블 검토 시 진행.
 * 스펙: https://openapi.krx.co.kr
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KrxApiClient {

    private static final DateTimeFormatter KRX_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 유가증권 일별매매정보 BO_ID (서비스별 이용신청 필요) */
    private static final String BO_ID_STOCK_KOSPI = "JvJFzlAENzZlPBDNGAWC";

    private final RestTemplate restTemplate = new RestTemplate();
    private final DataCollectionProperties dataCollectionProperties;

    /**
     * 유가증권 일별매매정보 조회 (기준일자)
     * 인증키 미설정 시 빈 리스트 반환. API 실패 시 Fallback으로 빈 리스트.
     *
     * @param basDt 기준일자 (YYYYMMDD)
     * @return OutBlock_1 리스트. 실패 시 빈 리스트
     */
    public List<Map<String, Object>> fetchDailyStockKospi(LocalDate basDt) {
        String authKey = dataCollectionProperties.getKrx().getAuthKey();
        if (authKey == null || authKey.isBlank()) {
            log.debug("KRX AUTH_KEY 미설정: 조회 스킵");
            return Collections.emptyList();
        }

        String baseUrl = dataCollectionProperties.getKrx().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://openapi.krx.co.kr";
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/contents/OPP/USES/service/OPPUSES002_S2.cmd")
                .queryParam("BO_ID", BO_ID_STOCK_KOSPI)
                .queryParam("basDt", basDt.format(KRX_DATE))
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("AUTH_KEY", authKey);

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
            log.warn("KRX API 호출 실패: basDt={}, error={}", basDt, e.getMessage());
            return Collections.emptyList();
        }
    }
}
