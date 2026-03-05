package com.investment.api.controller;

import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.risk.dto.MacroIndicatorDto;
import com.investment.risk.service.MacroDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 매크로 지표 대시보드 REST API.
 */
@RestController
@RequestMapping("/api/v1/macro")
@RequiredArgsConstructor
@Tag(name = "Macro", description = "거시경제 지표 대시보드 API")
public class MacroController {

    private final MacroDashboardService macroDashboardService;

    @GetMapping("/dashboard")
    @Operation(summary = "매크로 대시보드", description = "전체 거시경제 지표와 시장 상태를 조회합니다.")
    public ResponseEntity<MacroDashboardResponse> getDashboard() {
        try {
            MacroDashboardResponse response = macroDashboardService.getDashboard();
            return ResponseEntity.ok(response != null ? response : buildEmptyDashboardResponse());
        } catch (Exception e) {
            return ResponseEntity.ok(buildEmptyDashboardResponse());
        }
    }

    @GetMapping("/indicators/{code}")
    @Operation(summary = "개별 지표 조회", description = "특정 거시경제 지표를 조회합니다.")
    public ResponseEntity<MacroIndicatorDto> getIndicator(
            @Parameter(description = "지표 코드 (VIX, US10Y, CPI 등)") @PathVariable String code) {
        MacroIndicatorDto indicator = macroDashboardService.getIndicator(code);
        if (indicator == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(indicator);
    }

    @GetMapping("/indicators/{code}/history")
    @Operation(summary = "지표 히스토리", description = "지표의 기간별 히스토리를 조회합니다.")
    public ResponseEntity<List<MacroIndicatorDto>> getIndicatorHistory(
            @Parameter(description = "지표 코드") @PathVariable String code,
            @Parameter(description = "시작일 (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일 (YYYY-MM-DD)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<MacroIndicatorDto> history = macroDashboardService.getIndicatorHistory(code, startDate, endDate);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/regime")
    @Operation(summary = "시장 상태 조회", description = "현재 시장 상태(레짐)를 조회합니다.")
    public ResponseEntity<RegimeResponse> getMarketRegime() {
        try {
            MacroDashboardResponse dashboard = macroDashboardService.getDashboard();
            if (dashboard == null) {
                return ResponseEntity.ok(new RegimeResponse(
                        MacroDashboardResponse.MarketRegime.NEUTRAL, 0.0, 50, "횡보장: 데이터 없음"));
            }
            MacroDashboardResponse.MarketRegime regime = dashboard.getRegime();
            return ResponseEntity.ok(new RegimeResponse(
                    regime,
                    dashboard.getRegimeConfidence() != null ? dashboard.getRegimeConfidence() : 0.0,
                    dashboard.getOverallRiskScore() != null ? dashboard.getOverallRiskScore() : 50,
                    getRegimeDescription(regime)
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(new RegimeResponse(
                    MacroDashboardResponse.MarketRegime.NEUTRAL, 0.0, 50, "횡보장: 데이터 없음"));
        }
    }

    @GetMapping("/indicators")
    @Operation(summary = "지원 지표 목록", description = "조회 가능한 모든 지표 코드 목록을 반환합니다.")
    public ResponseEntity<List<String>> getSupportedIndicators() {
        return ResponseEntity.ok(macroDashboardService.getSupportedIndicatorCodes());
    }

    @PostMapping("/refresh")
    @Operation(summary = "캐시 갱신", description = "매크로 대시보드 캐시를 강제로 갱신합니다.")
    public ResponseEntity<Void> refreshCache() {
        macroDashboardService.refreshCache();
        return ResponseEntity.ok().build();
    }

    public record RegimeResponse(
            MacroDashboardResponse.MarketRegime regime,
            Double confidence,
            Integer riskScore,
            String description
    ) {}

    private String getRegimeDescription(MacroDashboardResponse.MarketRegime regime) {
        return switch (regime) {
            case BULL -> "상승장: 낮은 변동성, 리스크 온 환경";
            case BEAR -> "하락장: 높은 변동성, 리스크 오프 권장";
            case NEUTRAL -> "횡보장: 중립적 환경, 선별적 투자 권장";
        };
    }

    private static MacroDashboardResponse buildEmptyDashboardResponse() {
        return MacroDashboardResponse.builder()
                .regime(MacroDashboardResponse.MarketRegime.NEUTRAL)
                .regimeConfidence(0.0)
                .overallRiskScore(50)
                .marketIndicators(Collections.emptyList())
                .interestRateIndicators(Collections.emptyList())
                .economyIndicators(Collections.emptyList())
                .currencyIndicators(Collections.emptyList())
                .allIndicators(Collections.emptyMap())
                .riskGateStatus(MacroDashboardResponse.RiskGateStatus.builder()
                        .enabled(false)
                        .triggered(false)
                        .build())
                .timestamp(Instant.now())
                .cached(false)
                .build();
    }
}
