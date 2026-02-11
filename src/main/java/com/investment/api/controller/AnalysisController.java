package com.investment.api.controller;

import com.investment.analysis.dto.AnalysisRequestDto;
import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.analysis.dto.CorrelationAnalysisResponseDto;
import com.investment.analysis.dto.SectorAnalysisResponseDto;
import com.investment.analysis.service.AnalysisService;
import com.investment.analysis.service.CorrelationAnalysisService;
import com.investment.analysis.service.SectorAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 분석 및 섹터 분석 REST API
 */
@Tag(name = "Analysis", description = "종목 분석·섹터 분석 API")
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final SectorAnalysisService sectorAnalysisService;
    private final CorrelationAnalysisService correlationAnalysisService;

    @Operation(
            summary = "종목 분석",
            description = "AI 기반 종목 분석 및 투자 추천을 수행합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "분석 성공",
                    content = @Content(schema = @Schema(implementation = AnalysisResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping
    public ResponseEntity<AnalysisResponseDto> analyze(
            @RequestBody @Valid AnalysisRequestDto request) {
        AnalysisResponseDto response = analysisService.analyze(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "섹터 분석",
            description = "계좌 포지션 또는 종목 목록의 섹터별 비중·수익 기여도를 반환합니다. accountNo가 있으면 해당 계좌 보유 종목 기준, 없으면 symbols·market 쿼리로 종목별 비중 분석."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "섹터 분석 성공",
                    content = @Content(schema = @Schema(implementation = SectorAnalysisResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (accountNo 사용 시)")
    })
    @GetMapping("/sector")
    public ResponseEntity<SectorAnalysisResponseDto> getSectorAnalysis(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String symbols,
            @RequestParam(required = false, defaultValue = "US") String market) {
        if (accountNo != null && !accountNo.isBlank()) {
            String userId = userDetails != null ? userDetails.getUsername() : null;
            SectorAnalysisResponseDto response = sectorAnalysisService.getSectorAnalysisByAccount(userId, accountNo.trim());
            return ResponseEntity.ok(response);
        }
        if (symbols != null && !symbols.isBlank()) {
            Map<String, BigDecimal> symbolValues = Arrays.stream(symbols.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toMap(s -> s, s -> BigDecimal.ONE));
            SectorAnalysisResponseDto response = sectorAnalysisService.getSectorAnalysisBySymbols(market, symbolValues);
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.ok(sectorAnalysisService.getSectorAnalysisBySymbols(market, Collections.emptyMap()));
    }

    @Operation(
            summary = "상관관계 분석",
            description = "계좌 포지션 또는 종목 목록의 일봉 수익률 기반 Pearson 상관계수 행렬을 반환합니다. accountNo가 있으면 해당 계좌 보유 종목 기준, 없으면 symbols·market·from·to 쿼리로 분석. 최소 2종목·20일 이상 데이터 필요."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상관관계 분석 성공",
                    content = @Content(schema = @Schema(implementation = CorrelationAnalysisResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증 필요 (accountNo 사용 시)")
    })
    @GetMapping("/correlation")
    public ResponseEntity<CorrelationAnalysisResponseDto> getCorrelationAnalysis(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String symbols,
            @RequestParam(required = false, defaultValue = "US") String market,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate to) {
        if (accountNo != null && !accountNo.isBlank()) {
            String userId = userDetails != null ? userDetails.getUsername() : null;
            CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationByAccount(
                    userId, accountNo.trim(), from, to);
            return ResponseEntity.ok(response);
        }
        List<String> symbolList = (symbols != null && !symbols.isBlank())
                ? Arrays.stream(symbols.split(",")).map(String::trim).filter(s -> !s.isEmpty()).distinct().collect(Collectors.toList())
                : Collections.emptyList();
        CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationBySymbols(
                symbolList, market, from, to);
        return ResponseEntity.ok(response);
    }
}
