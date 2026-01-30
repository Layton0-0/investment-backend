package com.investment.datacollection.client;

import com.investment.common.security.LogMaskingUtil;
import com.investment.config.DataCollectionProperties;
import com.investment.datacollection.dto.DartListResponseDto;
import com.investment.datacollection.dto.DartListItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Open DART 공시 목록 API 클라이언트
 * 스펙: https://opendart.fss.or.kr/guide/main.do?apiGrpCd=DS001 (list.json)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DartApiClient {

    private static final String LIST_PATH = "/list.json";
    private static final DateTimeFormatter DART_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int MAX_PAGE_COUNT = 100;

    private final RestTemplate restTemplate = new RestTemplate();
    private final DataCollectionProperties dataCollectionProperties;

    /**
     * 기간별 공시 목록 조회 (페이지네이션)
     *
     * @param bgnDe 시작일 (YYYYMMDD)
     * @param endDe 종료일 (YYYYMMDD)
     * @param pageNo 페이지 번호 (1부터)
     * @return 공시 목록 (최대 100건). API 실패 시 빈 리스트
     */
    public List<DartListItemDto> fetchList(LocalDate bgnDe, LocalDate endDe, int pageNo) {
        String apiKey = dataCollectionProperties.getDart().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("DART API 키 미설정: 수집 스킵");
            return List.of();
        }

        String baseUrl = dataCollectionProperties.getDart().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://opendart.fss.or.kr/api";
        }
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + LIST_PATH)
                .queryParam("crtfc_key", apiKey)
                .queryParam("bgn_de", bgnDe.format(DART_DATE))
                .queryParam("end_de", endDe.format(DART_DATE))
                .queryParam("page_no", pageNo)
                .queryParam("page_count", MAX_PAGE_COUNT)
                .build()
                .toUriString();

        try {
            ResponseEntity<DartListResponseDto> response = restTemplate.getForEntity(url, DartListResponseDto.class);
            DartListResponseDto body = response.getBody();
            if (body == null || !body.isSuccess()) {
                log.warn("DART API 응답 오류: status={}, message={}", body != null ? body.getStatus() : null, body != null ? body.getMessage() : null);
                return List.of();
            }
            List<DartListItemDto> list = body.getList();
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.warn("DART API 호출 실패: bgnDe={}, endDe={}, pageNo={}, error={}", bgnDe, endDe, pageNo, e.getMessage());
            return List.of();
        }
    }

    /**
     * 최근 N일 공시 전체 조회 (여러 페이지)
     */
    public List<DartListItemDto> fetchListForDays(int days) {
        LocalDate endDe = LocalDate.now();
        LocalDate bgnDe = endDe.minusDays(days);
        List<DartListItemDto> all = new ArrayList<>();
        int pageNo = 1;
        while (true) {
            List<DartListItemDto> page = fetchList(bgnDe, endDe, pageNo);
            if (page.isEmpty()) {
                break;
            }
            all.addAll(page);
            if (page.size() < MAX_PAGE_COUNT) {
                break;
            }
            pageNo++;
        }
        log.debug("DART 공시 수집: bgnDe={}, endDe={}, total={}", bgnDe, endDe, all.size());
        return all;
    }

    /**
     * 공시 뷰어 URL 생성 (한국어 DART)
     */
    public static String buildViewerUrl(String rceptNo) {
        if (rceptNo == null || rceptNo.isBlank()) {
            return "";
        }
        return "https://dart.fss.or.kr/dsbh001/main.do?rcpNo=" + rceptNo;
    }
}
