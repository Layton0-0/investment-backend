package com.investment.datacollection.client;

import com.investment.config.DataCollectionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Python investment-data-collector HTTP API 호출 (수동 수집용).
 * DART/SEC 공시 수집은 수집기 POST /dart-collect, /sec-collect 로 위임.
 * nginx proxy_read_timeout(60s) 내에 응답하도록 connect/read 타임아웃 설정.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectorApiClient {

    private static final String PATH_DART = "/dart-collect";
    private static final String PATH_SEC = "/sec-collect";
    /** 수집기 연결 타임아웃(ms). 미연결 시 빠르게 실패 */
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    /** 수집기 읽기 타임아웃(ms). DART+SEC 두 호출 합쳐 nginx 60s 이내 되도록 호출당 25s */
    private static final int READ_TIMEOUT_MS = 25_000;

    private final DataCollectionProperties dataCollectionProperties;
    private final RestTemplate restTemplate = createRestTemplateWithTimeouts();

    private static RestTemplate createRestTemplateWithTimeouts() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout(READ_TIMEOUT_MS);
        return new RestTemplate(factory);
    }

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
