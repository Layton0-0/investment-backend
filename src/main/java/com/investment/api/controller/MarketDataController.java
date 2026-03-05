package com.investment.api.controller;

import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.dto.DailyChartPointDto;
import com.investment.marketdata.dto.SymbolSearchItemDto;
import com.investment.marketdata.service.DailyChartService;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.investment.marketdata.service.SymbolSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.NotBlank;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * 시장 데이터 REST API
 * 
 * 실시간 시세 조회 API를 제공합니다.
 */
@Slf4j
@Tag(name = "Market Data", description = "시장 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final RealtimeMarketDataService realtimeMarketDataService;
    private final DailyChartService dailyChartService;
    private final SymbolSearchService symbolSearchService;

    /** 진입 확인용. GET /api/v1/market-data/ping → 200 "ok" (daily-chart 404 시 컨트롤러 도달 여부 확인). */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    @Operation(
            summary = "단일 종목 현재가 조회",
            description = "한국투자증권 API를 통해 단일 종목의 실시간 현재가 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CurrentPriceDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "종목을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @GetMapping("/current-price/{symbol}")
    public ResponseEntity<CurrentPriceDto> getCurrentPrice(
            @Parameter(description = "종목 코드 (6자리 또는 종목명)", required = true, example = "005930")
            @PathVariable @NotBlank String symbol) {
        CurrentPriceDto currentPrice = realtimeMarketDataService.getCurrentPrice(symbol)
                .block(Duration.ofSeconds(10));
        if (currentPrice == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(currentPrice);
    }
    
    @Operation(
            summary = "여러 종목 현재가 일괄 조회",
            description = "한국투자증권 API를 통해 여러 종목의 실시간 현재가 정보를 일괄 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = CurrentPriceDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping("/current-prices")
    public ResponseEntity<List<CurrentPriceDto>> getCurrentPrices(
            @Parameter(description = "종목 코드 목록", required = true)
            @RequestBody List<@NotBlank String> symbols) {
        try {
            List<CurrentPriceDto> currentPrices = realtimeMarketDataService.getCurrentPrices(symbols)
                    .block(Duration.ofSeconds(30));
            return ResponseEntity.ok(currentPrices != null ? currentPrices : List.of());
        } catch (Exception e) {
            log.warn("current-prices 일괄 조회 실패: symbols={}, error={}", symbols, e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    @Operation(
            summary = "일봉 차트 조회",
            description = "TB_DAILY_STOCK 기반 종목·시장·기간별 일봉 데이터. from/to 미지정 시 최근 1년, 최대 365일."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공 (데이터 없으면 빈 배열)",
                    content = @Content(schema = @Schema(implementation = DailyChartPointDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (파라미터)")
    })
    @GetMapping("/daily-chart")
    public ResponseEntity<List<DailyChartPointDto>> getDailyChart(
            @Parameter(description = "종목 코드", required = true, example = "005930") @RequestParam @NotBlank String symbol,
            @Parameter(description = "시장 (KR, US)", required = true, example = "KR") @RequestParam(defaultValue = "KR") String market,
            @Parameter(description = "시작일 (yyyy-MM-dd)") @RequestParam(required = false) String from,
            @Parameter(description = "종료일 (yyyy-MM-dd)") @RequestParam(required = false) String to) {
        LocalDate fromDate = null;
        LocalDate toDate = null;
        try {
            if (from != null && !from.isBlank()) {
                fromDate = LocalDate.parse(from);
            }
            if (to != null && !to.isBlank()) {
                toDate = LocalDate.parse(to);
            }
        } catch (Exception e) {
            log.warn("daily-chart 파라미터 파싱 실패: from={}, to={}", from, to, e);
            return ResponseEntity.badRequest().build();
        }
        List<DailyChartPointDto> list = dailyChartService.getDailyChart(symbol, market, fromDate, toDate);
        return ResponseEntity.ok(list);
    }

    @Operation(
            summary = "종목 통합 검색",
            description = "종목 코드 또는 종목명으로 검색. KR/US 시장별 또는 전체 검색. 수동 주문 등에서 종목 선택용."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = SymbolSearchItemDto.class)))
    })
    @GetMapping("/symbols/search")
    public ResponseEntity<List<SymbolSearchItemDto>> searchSymbols(
            @Parameter(description = "검색어 (종목코드 또는 종목명, 비면 전체)") @RequestParam(required = false) String q,
            @Parameter(description = "시장 (KR, US, 미지정 시 전체)") @RequestParam(required = false) String market) {
        List<SymbolSearchItemDto> list = symbolSearchService.search(q, market);
        return ResponseEntity.ok(list);
    }
}
