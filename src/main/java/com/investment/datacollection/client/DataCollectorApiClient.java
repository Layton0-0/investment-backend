package com.investment.datacollection.client;

import com.investment.config.DataCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Python investment-data-collector HTTP API 호출 (수동 수집용).
 * DART/SEC 공시 수집은 수집기 POST /dart-collect, /sec-collect 로 위임.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectorApiClient {

    private static final String PATH_DART = "/dart-collect";
    private static final String PATH_SEC = "/sec-collect";

    private final DataCollectionProperties dataCollectionProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 수집기 베이스 URL (investment.data.us.collector-url). 비어 있으면 수집 불가.
     */
    public String getCollectorBaseUrl() {
        String url = dataCollectionProperties.getUs().getCollectorUrl();
        return url != null ? url.strip().replaceAll("/$", "") : "";
    }

    /**
     * DART 공시 수집 트리거. Python 수집기 POST /dart-collect 호출.
     *
     * @return 저장된 건수. 수집기 URL 미설정 또는 실패 시 0
     */
    public int triggerDartCollect() {
        String base = getCollectorBaseUrl();
        if (base.isEmpty()) {
            log.warn("수동 DART 수집 스킵: collector-url 미설정");
            return 0;
        }
        String url = base + PATH_DART;
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("saved")) {
                Object saved = response.getBody().get("saved");
                return saved instanceof Number ? ((Number) saved).intValue() : 0;
            }
            return 0;
        } catch (Exception e) {
            log.warn("수동 DART 수집 실패: url={}, error={}", url, e.getMessage());
            return 0;
        }
    }

    /**
     * SEC EDGAR 공시 수집 트리거. Python 수집기 POST /sec-collect 호출.
     *
     * @return 저장된 건수. 수집기 URL 미설정 또는 실패 시 0
     */
    public int triggerSecCollect() {
        String base = getCollectorBaseUrl();
        if (base.isEmpty()) {
            log.warn("수동 SEC 수집 스킵: collector-url 미설정");
            return 0;
        }
        String url = base + PATH_SEC;
        try {
            @SuppressWarnings("unchecked")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("saved")) {
                Object saved = response.getBody().get("saved");
                return saved instanceof Number ? ((Number) saved).intValue() : 0;
            }
            return 0;
        } catch (Exception e) {
            log.warn("수동 SEC 수집 실패: url={}, error={}", url, e.getMessage());
            return 0;
        }
    }
}
