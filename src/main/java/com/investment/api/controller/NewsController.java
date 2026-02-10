package com.investment.api.controller;

import com.investment.datacollection.client.DataCollectorApiClient;
import com.investment.news.dto.NewsItemPageResponseDto;
import com.investment.news.service.NewsItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Tag(name = "News", description = "뉴스·공시 API")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsItemService newsItemService;
    private final DataCollectorApiClient dataCollectorApiClient;

    @Operation(summary = "뉴스·공시 목록 조회", description = "필터(시장·원천·유형·종목·제목·기간) 및 페이징으로 뉴스·공시 목록을 조회합니다. 빈값이면 해당 조건 전체.")
    @GetMapping
    public ResponseEntity<NewsItemPageResponseDto> getNews(
            @Parameter(description = "시장 (KR, US)") @RequestParam(required = false) String market,
            @Parameter(description = "원천 코드 (DART, SEC_EDGAR, YONHAP 등)") @RequestParam(required = false) String source,
            @Parameter(description = "유형 (FACT, SPEED, BUZZ)") @RequestParam(required = false) String itemType,
            @Parameter(description = "연관 종목 코드") @RequestParam(required = false) String symbol,
            @Parameter(description = "제목 부분 일치") @RequestParam(required = false) String title,
            @Parameter(description = "수집 시작일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "수집 종료일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "페이지 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(required = false, defaultValue = "20") int size) {
        NewsItemPageResponseDto response = newsItemService.getNewsItems(market, source, itemType, symbol, title, from, to, page, size);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "뉴스·공시 수집 실행", description = "Python 수집기 API(POST /dart-collect, /sec-collect)를 호출해 DART·SEC EDGAR 공시를 즉시 수집합니다. investment.data.us.collector-url 설정 필요.")
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> triggerNewsAndDisclosureCollection() {
        if (dataCollectorApiClient.getCollectorBaseUrl().isEmpty()) {
            return ResponseEntity.status(503).body(Map.of(
                    "dartSaved", 0,
                    "secSaved", 0,
                    "message", "데이터 수집기 URL(investment.data.us.collector-url) 미설정. Python 수집기 기동 후 설정하세요."
            ));
        }
        int dartSaved = dataCollectorApiClient.triggerDartCollect();
        int secSaved = dataCollectorApiClient.triggerSecCollect();
        return ResponseEntity.ok(Map.of(
                "dartSaved", dartSaved,
                "secSaved", secSaved,
                "message", "뉴스·공시 수집이 완료되었습니다."
        ));
    }
}
