package com.investment.datacollection.client;

import com.investment.common.security.LogMaskingUtil;
import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.dto.SecEdgarItemDto;
import com.investment.datacollection.dto.SecSubmissionsResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * SEC EDGAR Submissions API 클라이언트 (data.sec.gov/submissions/CIK{cik}.json)
 * API 키 미설정 시 수집 스킵. User-Agent 필수, API 키는 X-SEC-API-Key 헤더로 전달.
 */
@Slf4j
@Component
public class SecEdgarApiClient {

    private static final String USER_AGENT = "InvestmentChoi/1.0 (SEC EDGAR data collection)";
    private static final String HEADER_SEC_API_KEY = "X-SEC-API-Key";
    private static final DateTimeFormatter SEC_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    /** 수집 대상 CIK (10자리, 선행 0). Apple, Microsoft, Amazon */
    private static final List<String> DEFAULT_CIKS = List.of("0000320193", "0000789019", "0001018724");

    private final DataCollectionProperties dataCollectionProperties;
    private final RestTemplate restTemplate;

    @Autowired
    public SecEdgarApiClient(DataCollectionProperties dataCollectionProperties) {
        this.dataCollectionProperties = dataCollectionProperties;
        this.restTemplate = new RestTemplate();
    }

    public SecEdgarApiClient(DataCollectionProperties dataCollectionProperties, RestTemplate restTemplate) {
        this.dataCollectionProperties = dataCollectionProperties;
        this.restTemplate = restTemplate;
    }

    /**
     * 최근 N일 이내 제출 건을 수집 (설정된 CIK 목록 기준)
     *
     * @param days 최근 일수
     * @return SEC EDGAR 항목 목록 (키 미설정 시 빈 리스트)
     */
    public List<SecEdgarItemDto> fetchRecentFilings(int days) {
        String apiKey = dataCollectionProperties.getSec().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SEC API 키 미설정: 수집 스킵");
            return List.of();
        }

        String baseUrl = dataCollectionProperties.getSec().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://data.sec.gov";
        }
        baseUrl = baseUrl.replaceAll("/$", "");

        LocalDate since = LocalDate.now().minusDays(days);
        List<SecEdgarItemDto> all = new ArrayList<>();

        for (String cik : DEFAULT_CIKS) {
            try {
                List<SecEdgarItemDto> items = fetchSubmissionsForCik(baseUrl, apiKey, cik, since);
                all.addAll(items);
            } catch (Exception e) {
                log.warn("SEC API 호출 실패: cik={}, error={}", cik, e.getMessage());
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("SEC API 수집: baseUrl={}, apiKey={}, total={}", baseUrl, LogMaskingUtil.maskApiKey(apiKey), all.size());
        }
        return all;
    }

    private List<SecEdgarItemDto> fetchSubmissionsForCik(String baseUrl, String apiKey, String cik, LocalDate since) {
        String url = baseUrl + "/submissions/CIK" + cik + ".json";
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", USER_AGENT);
        headers.set(HEADER_SEC_API_KEY, apiKey);

        ResponseEntity<SecSubmissionsResponseDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                SecSubmissionsResponseDto.class
        );

        SecSubmissionsResponseDto body = response.getBody();
        if (body == null || body.getFilings() == null || body.getFilings().getRecent() == null) {
            return List.of();
        }

        SecSubmissionsResponseDto.RecentFilings recent = body.getFilings().getRecent();
        List<String> accessionNumbers = recent.getAccessionNumber();
        List<String> forms = recent.getForm();
        List<String> filingDates = recent.getFilingDate();
        List<String> primaryDocs = recent.getPrimaryDocument();
        if (accessionNumbers == null || accessionNumbers.isEmpty()) {
            return List.of();
        }

        String companyName = body.getName() != null ? body.getName() : "";
        String cikTrimmed = body.getCik() != null ? body.getCik() : cik;

        int size = accessionNumbers.size();
        List<SecEdgarItemDto> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String filingDateStr = i < filingDates.size() ? filingDates.get(i) : null;
            if (filingDateStr == null || filingDateStr.length() < 10) {
                continue;
            }
            LocalDate filingDate;
            try {
                filingDate = LocalDate.parse(filingDateStr.substring(0, 10), SEC_DATE);
            } catch (Exception e) {
                continue;
            }
            if (filingDate.isBefore(since)) {
                continue;
            }
            String accessionNumber = accessionNumbers.get(i);
            String form = i < forms.size() ? forms.get(i) : "";
            String primaryDoc = i < primaryDocs.size() ? primaryDocs.get(i) : null;
            result.add(SecEdgarItemDto.builder()
                    .accessionNumber(accessionNumber)
                    .form(form)
                    .filingDate(filingDateStr)
                    .primaryDocument(primaryDoc != null && !primaryDoc.isBlank() ? primaryDoc : "")
                    .cik(cikTrimmed)
                    .companyName(companyName)
                    .build());
        }
        return result;
    }

    /**
     * SEC EDGAR 문서 URL 생성 (Archives)
     * 형식: https://www.sec.gov/Archives/edgar/data/{cik}/{accession_no_dashes}/{primaryDocument}
     */
    public static String buildDocumentUrl(String cik, String accessionNumber, String primaryDocument) {
        if (cik == null || accessionNumber == null) {
            return "";
        }
        String accessionNoDashes = accessionNumber.replace("-", "");
        String doc = (primaryDocument != null && !primaryDocument.isBlank()) ? primaryDocument : accessionNoDashes + ".htm";
        return "https://www.sec.gov/Archives/edgar/data/" + cik + "/" + accessionNoDashes + "/" + doc;
    }
}
