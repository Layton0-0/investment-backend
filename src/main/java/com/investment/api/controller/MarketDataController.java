package com.investment.api.controller;

import com.investment.marketdata.dto.CurrentPriceDto;
import com.investment.marketdata.service.RealtimeMarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.util.List;

/**
 * 시장 데이터 REST API
 * 
 * 실시간 시세 조회 API를 제공합니다.
 */
@Tag(name = "Market Data", description = "시장 데이터 조회 API")
@RestController
@RequestMapping("/api/v1/market-data")
@RequiredArgsConstructor
public class MarketDataController {
    
    private final RealtimeMarketDataService realtimeMarketDataService;
    
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
        List<CurrentPriceDto> currentPrices = realtimeMarketDataService.getCurrentPrices(symbols)
                .block(Duration.ofSeconds(30));
        return ResponseEntity.ok(currentPrices);
    }
}
