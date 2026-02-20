package com.investment.api.controller;

import com.investment.core.engine.execution.TransactionCostAnalyzer;
import com.investment.core.engine.execution.dto.TcaEstimateRequest;
import com.investment.core.engine.execution.dto.TcaReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Transaction Cost Analysis (TCA) REST API.
 */
@RestController
@RequestMapping("/api/v1/tca")
@RequiredArgsConstructor
@Tag(name = "TCA", description = "Transaction Cost Analysis API")
public class TcaController {

    private final TransactionCostAnalyzer transactionCostAnalyzer;

    @PostMapping("/estimate")
    @Operation(summary = "사전 비용 예측", description = "주문 실행 전 예상 거래 비용을 계산합니다.")
    public ResponseEntity<TcaReport> estimateCost(@RequestBody TcaEstimateRequestDto request) {
        TcaEstimateRequest tcaRequest = TcaEstimateRequest.builder()
                .symbol(request.symbol())
                .market(request.market())
                .assetType(request.assetType())
                .side(request.side())
                .quantity(request.quantity())
                .arrivalPrice(request.arrivalPrice())
                .avgDailyVolume(request.avgDailyVolume())
                .avgSpreadPct(request.avgSpreadPct())
                .build();

        TcaReport report = transactionCostAnalyzer.estimateCost(tcaRequest);

        if (!report.isValid()) {
            return ResponseEntity.badRequest().body(report);
        }
        return ResponseEntity.ok(report);
    }

    @PostMapping("/analyze")
    @Operation(summary = "사후 비용 분석", description = "실제 체결 결과와 도착가를 비교하여 Implementation Shortfall을 계산합니다.")
    public ResponseEntity<TcaReport> analyzeCost(@RequestBody TcaAnalyzeRequestDto request) {
        TcaReport report = transactionCostAnalyzer.analyzeCost(
                request.symbol(),
                request.market(),
                request.side(),
                request.quantity(),
                request.arrivalPrice(),
                request.executionPrice(),
                request.executionCost()
        );

        if (!report.isValid()) {
            return ResponseEntity.badRequest().body(report);
        }
        return ResponseEntity.ok(report);
    }

    @GetMapping("/round-trip")
    @Operation(summary = "왕복 거래 비용", description = "매수+매도 시 발생하는 총 거래 비용을 계산합니다.")
    public ResponseEntity<RoundTripCostResponse> calculateRoundTripCost(
            @Parameter(description = "시장 (KR, US)") @RequestParam String market,
            @Parameter(description = "자산 유형 (STOCK, ETF)") @RequestParam(defaultValue = "STOCK") String assetType,
            @Parameter(description = "거래 금액") @RequestParam BigDecimal notional,
            @Parameter(description = "수량 (TAF 계산용)") @RequestParam(defaultValue = "100") int quantity) {

        BigDecimal cost = transactionCostAnalyzer.calculateRoundTripCost(market, assetType, notional, quantity);
        BigDecimal costPct = notional.compareTo(BigDecimal.ZERO) > 0
                ? cost.divide(notional, 6, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        return ResponseEntity.ok(new RoundTripCostResponse(market, assetType, notional, quantity, cost, costPct));
    }

    @GetMapping("/market-impact")
    @Operation(summary = "시장 충격 비용 예측", description = "주문 수량에 따른 시장 충격 비용을 예측합니다.")
    public ResponseEntity<MarketImpactResponse> estimateMarketImpact(
            @Parameter(description = "주문 수량") @RequestParam int orderQuantity,
            @Parameter(description = "일평균 거래량 (ADV)") @RequestParam long avgDailyVolume,
            @Parameter(description = "변동성 (기본 0.02)") @RequestParam(required = false) BigDecimal volatility) {

        BigDecimal impact = transactionCostAnalyzer.estimateMarketImpact(
                orderQuantity, avgDailyVolume, volatility);
        double participationRate = avgDailyVolume > 0 ? (double) orderQuantity / avgDailyVolume * 100 : 0;

        return ResponseEntity.ok(new MarketImpactResponse(
                orderQuantity, avgDailyVolume, participationRate,
                impact.multiply(new BigDecimal("100")).setScale(4, java.math.RoundingMode.HALF_UP)));
    }

    public record TcaEstimateRequestDto(
            String symbol,
            String market,
            String assetType,
            String side,
            int quantity,
            BigDecimal arrivalPrice,
            Long avgDailyVolume,
            BigDecimal avgSpreadPct
    ) {}

    public record TcaAnalyzeRequestDto(
            String symbol,
            String market,
            String side,
            int quantity,
            BigDecimal arrivalPrice,
            BigDecimal executionPrice,
            BigDecimal executionCost
    ) {}

    public record RoundTripCostResponse(
            String market,
            String assetType,
            BigDecimal notional,
            int quantity,
            BigDecimal roundTripCost,
            BigDecimal roundTripCostPct
    ) {}

    public record MarketImpactResponse(
            int orderQuantity,
            long avgDailyVolume,
            double participationRatePct,
            BigDecimal marketImpactPct
    ) {}
}
