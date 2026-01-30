package com.investment.api.controller;

import com.investment.news.dto.NewsItemPageResponseDto;
import com.investment.news.service.NewsItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "News", description = "뉴스·공시 API")
@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsItemService newsItemService;

    @Operation(summary = "뉴스·공시 목록 조회", description = "필터(시장·원천·종목·기간) 및 페이징으로 뉴스·공시 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<NewsItemPageResponseDto> getNews(
            @Parameter(description = "시장 (KR, US)") @RequestParam(required = false) String market,
            @Parameter(description = "원천 코드 (DART, SEC_EDGAR, YONHAP 등)") @RequestParam(required = false) String source,
            @Parameter(description = "연관 종목 코드") @RequestParam(required = false) String symbol,
            @Parameter(description = "수집 시작일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "수집 종료일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "페이지 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(required = false, defaultValue = "20") int size) {
        NewsItemPageResponseDto response = newsItemService.getNewsItems(market, source, symbol, from, to, page, size);
        return ResponseEntity.ok(response);
    }
}
