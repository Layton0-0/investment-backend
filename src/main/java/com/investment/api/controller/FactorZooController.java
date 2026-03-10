package com.investment.api.controller;

import com.investment.factor.zoo.FactorDefinition;
import com.investment.factor.zoo.FactorTestResult;
import com.investment.factor.zoo.FactorZooService;
import com.investment.factor.zoo.FactorZooService.CombinedFactorScore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Factor Zoo REST API.
 */
@RestController
@RequestMapping("/api/v1/factor-zoo")
@RequiredArgsConstructor
@Tag(name = "FactorZoo", description = "Factor Zoo - 팩터 테스트 및 랭킹 API")
public class FactorZooController {

    private final FactorZooService factorZooService;

    @GetMapping("/test/{factorCode}")
    @Operation(summary = "단일 팩터 테스트", description = "특정 팩터의 성과를 테스트합니다.")
    public ResponseEntity<FactorTestResult> testFactor(
            @Parameter(description = "팩터 코드 (PBR, MOMENTUM_3M 등)") @PathVariable String factorCode,
            @Parameter(description = "시장 (KR, US)") @RequestParam(defaultValue = "KR") String market,
            @Parameter(description = "시작일") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            FactorTestResult result = factorZooService.testFactor(factorCode, market, startDate, endDate);
            return ResponseEntity.ok(result != null ? result : FactorTestResult.error(factorCode, "No result"));
        } catch (Exception e) {
            return ResponseEntity.ok(FactorTestResult.error(factorCode, e.getMessage() != null ? e.getMessage() : "Error"));
        }
    }

    @GetMapping("/rank")
    @Operation(summary = "팩터 랭킹", description = "모든 팩터의 성과를 IC 순으로 랭킹합니다.")
    public ResponseEntity<List<FactorTestResult>> rankFactors(
            @Parameter(description = "시장 (KR, US)") @RequestParam(defaultValue = "KR") String market,
            @Parameter(description = "시작일") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<FactorTestResult> results = factorZooService.rankFactors(market, startDate, endDate);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/combined-score")
    @Operation(summary = "복합 팩터 점수", description = "가중치 기반 복합 팩터 점수를 계산합니다.")
    public ResponseEntity<CombinedFactorScore> getCombinedScore(
            @RequestBody CombinedScoreRequest request) {

        CombinedFactorScore score = factorZooService.getCombinedScore(
                request.symbol(),
                request.market(),
                request.basDt(),
                request.factorWeights()
        );
        return ResponseEntity.ok(score);
    }

    @PostMapping("/rank-stocks")
    @Operation(summary = "종목 랭킹", description = "복합 팩터 점수로 종목을 랭킹합니다.")
    public ResponseEntity<List<CombinedFactorScore>> rankStocksByFactors(
            @RequestBody StockRankingRequest request) {

        List<CombinedFactorScore> scores = factorZooService.rankStocksByFactors(
                request.market(),
                request.basDt(),
                request.factorWeights(),
                request.topN() != null ? request.topN() : 20
        );
        return ResponseEntity.ok(scores);
    }

    @GetMapping("/factors")
    @Operation(summary = "팩터 목록", description = "지원하는 모든 팩터 정의를 조회합니다.")
    public ResponseEntity<List<FactorDefinition>> getAllFactors() {
        return ResponseEntity.ok(factorZooService.getAllFactorDefinitions());
    }

    @GetMapping("/factors/{factorCode}")
    @Operation(summary = "팩터 상세", description = "특정 팩터의 정의를 조회합니다.")
    public ResponseEntity<FactorDefinition> getFactorDefinition(
            @PathVariable String factorCode) {

        FactorDefinition definition = factorZooService.getFactorDefinition(factorCode);
        if (definition == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(definition);
    }

    @GetMapping("/factors/category/{category}")
    @Operation(summary = "카테고리별 팩터", description = "특정 카테고리의 팩터 목록을 조회합니다.")
    public ResponseEntity<List<FactorDefinition>> getFactorsByCategory(
            @Parameter(description = "카테고리 (VALUE, MOMENTUM, QUALITY, SIZE, VOLATILITY, LIQUIDITY, TECHNICAL, FLOW)")
            @PathVariable String category) {

        try {
            FactorDefinition.FactorCategory cat = FactorDefinition.FactorCategory.valueOf(category.toUpperCase());
            return ResponseEntity.ok(factorZooService.getFactorsByCategory(cat));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/codes")
    @Operation(summary = "팩터 코드 목록", description = "지원하는 모든 팩터 코드를 조회합니다.")
    public ResponseEntity<List<String>> getSupportedFactorCodes() {
        return ResponseEntity.ok(factorZooService.getSupportedFactorCodes());
    }

    public record CombinedScoreRequest(
            String symbol,
            String market,
            LocalDate basDt,
            Map<String, BigDecimal> factorWeights
    ) {}

    public record StockRankingRequest(
            String market,
            LocalDate basDt,
            Map<String, BigDecimal> factorWeights,
            Integer topN
    ) {}
}
