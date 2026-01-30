package com.investment.api.controller;

import com.investment.factor.dto.SignalScorePageResponseDto;
import com.investment.factor.service.SignalScoreService;
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

@Tag(name = "Signal", description = "시그널/팩터 점수 API")
@RestController
@RequestMapping("/api/v1/signals")
@RequiredArgsConstructor
public class SignalController {

    private final SignalScoreService signalScoreService;

    @Operation(summary = "시그널/팩터 점수 목록 조회", description = "기준일·시장·종목·팩터 유형 필터 및 페이징으로 시그널 점수 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<SignalScorePageResponseDto> getSignals(
            @Parameter(description = "기준일 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate basDt,
            @Parameter(description = "시장 (KR, US)") @RequestParam(required = false) String market,
            @Parameter(description = "종목코드") @RequestParam(required = false) String symbol,
            @Parameter(description = "팩터 유형 (DISPARITY, VOLATILITY_BREAKOUT, LIQUIDITY 등)") @RequestParam(required = false) String factorType,
            @Parameter(description = "페이지 (0부터)") @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (최대 100)") @RequestParam(required = false, defaultValue = "20") int size) {
        SignalScorePageResponseDto response = signalScoreService.getSignals(basDt, market, symbol, factorType, page, size);
        return ResponseEntity.ok(response);
    }
}
