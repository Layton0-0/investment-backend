package com.investment.api.controller;

import com.investment.risk.dto.StressScenario;
import com.investment.risk.dto.StressTestResult;
import com.investment.risk.service.StressTestService;
import com.investment.risk.service.StressTestService.PortfolioPosition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 스트레스 테스트 REST API.
 */
@RestController
@RequestMapping("/api/v1/stress-test")
@RequiredArgsConstructor
@Tag(name = "StressTest", description = "Historical Stress Test API")
public class StressTestController {

    private final StressTestService stressTestService;

    @PostMapping("/run/{scenarioCode}")
    @Operation(summary = "단일 시나리오 스트레스 테스트", description = "특정 시나리오로 포트폴리오 스트레스 테스트를 실행합니다.")
    public ResponseEntity<StressTestResult> runStressTest(
            @PathVariable String scenarioCode,
            @RequestBody StressTestRequest request) {

        Map<String, PortfolioPosition> portfolio = convertToPortfolio(request.positions());
        StressTestResult result = stressTestService.runStressTest(portfolio, scenarioCode);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/run/all")
    @Operation(summary = "전체 시나리오 배치 테스트", description = "모든 기본 시나리오에 대해 스트레스 테스트를 실행합니다.")
    public ResponseEntity<List<StressTestResult>> runAllStressTests(
            @RequestBody StressTestRequest request) {

        Map<String, PortfolioPosition> portfolio = convertToPortfolio(request.positions());
        List<StressTestResult> results = stressTestService.runAllStressTests(portfolio);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/run/custom")
    @Operation(summary = "사용자 정의 시나리오 테스트", description = "사용자 정의 충격률로 스트레스 테스트를 실행합니다.")
    public ResponseEntity<StressTestResult> runCustomStressTest(
            @RequestBody CustomStressTestRequest request) {

        StressScenario customScenario = stressTestService.createCustomScenario(
                request.code(),
                request.name(),
                request.description(),
                request.assetClassShocks()
        );

        Map<String, PortfolioPosition> portfolio = convertToPortfolio(request.positions());
        StressTestResult result = stressTestService.runStressTest(portfolio, customScenario);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/scenarios")
    @Operation(summary = "지원 시나리오 목록", description = "사용 가능한 스트레스 테스트 시나리오 목록을 반환합니다.")
    public ResponseEntity<List<String>> getSupportedScenarios() {
        return ResponseEntity.ok(stressTestService.getSupportedScenarioCodes());
    }

    @GetMapping("/scenarios/{scenarioCode}")
    @Operation(summary = "시나리오 상세", description = "특정 시나리오의 상세 정보를 반환합니다.")
    public ResponseEntity<StressScenario> getScenario(@PathVariable String scenarioCode) {
        StressScenario scenario = stressTestService.getScenario(scenarioCode);
        if (scenario == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(scenario);
    }

    private Map<String, PortfolioPosition> convertToPortfolio(List<PositionDto> positions) {
        return positions.stream()
                .collect(Collectors.toMap(
                        PositionDto::symbol,
                        p -> new PortfolioPosition(
                                p.symbol(),
                                p.market() != null ? p.market() : "KR",
                                p.assetClass() != null ? p.assetClass() : "EQUITY",
                                p.value(),
                                p.quantity(),
                                p.currentPrice()
                        ),
                        (existing, replacement) -> existing
                ));
    }

    public record StressTestRequest(
            List<PositionDto> positions
    ) {}

    public record CustomStressTestRequest(
            String code,
            String name,
            String description,
            Map<String, BigDecimal> assetClassShocks,
            List<PositionDto> positions
    ) {}

    public record PositionDto(
            String symbol,
            String market,
            String assetClass,
            BigDecimal value,
            BigDecimal quantity,
            BigDecimal currentPrice
    ) {}
}
