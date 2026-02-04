package com.investment.api.controller;

import com.investment.backtest.BacktestService;
import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.backtest.robo.RoboBacktestService;
import com.investment.backtest.robo.RoboPreExecutionResultStore;
import com.investment.backtest.robo.dto.CollectUsDailyRequest;
import com.investment.backtest.robo.dto.CollectUsDailyResponse;
import com.investment.backtest.robo.dto.LastPreExecutionResultDto;
import com.investment.backtest.robo.dto.RoboBacktestRequest;
import com.investment.backtest.robo.dto.RoboBacktestResult;
import com.investment.config.RoboBacktestProperties;
import com.investment.datacollection.service.UsMarketCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Tag(name = "Backtest", description = "백테스트 API")
@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;
    private final RoboBacktestService roboBacktestService;
    private final RoboPreExecutionResultStore preExecutionResultStore;
    private final UsMarketCollectionService usMarketCollectionService;
    private final RoboBacktestProperties roboBacktestProperties;

    @Operation(summary = "백테스트 실행", description = "기간·시장·전략타입·초기자본으로 4단계 파이프라인 재생 후 메트릭·수익곡선·거래 목록 반환")
    @PostMapping
    public ResponseEntity<BacktestRunResult> run(@RequestBody @Valid BacktestRunRequest request) {
        BacktestRunResult result = backtestService.run(request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "로보어드바이저 백테스트 실행", description = "동적 자산배분(모멘텀·변동성 역가중) 백테스트. 메트릭·수익곡선·벤치마크·리밸런싱 이력 반환")
    @PostMapping("/robo")
    public ResponseEntity<RoboBacktestResult> runRobo(@RequestBody @Valid RoboBacktestRequest request) {
        RoboBacktestResult result = roboBacktestService.run(request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "실행 전 백테스트 최근 결과 조회", description = "로보 리밸런싱 스케줄러가 마지막으로 실행한 실행 전 백테스트 결과(통과/미통과·MDD·Sharpe)")
    @GetMapping("/robo/last-pre-execution")
    public ResponseEntity<LastPreExecutionResultDto> getLastPreExecution(
            @Parameter(description = "계좌번호") @RequestParam String accountNo) {
        RoboPreExecutionResultStore.StoredResult stored = preExecutionResultStore.get(accountNo);
        if (stored == null) {
            return ResponseEntity.noContent().build();
        }
        LastPreExecutionResultDto dto = LastPreExecutionResultDto.builder()
                .accountNo(accountNo)
                .passed(stored.isPassed())
                .mddPct(stored.getMddPct())
                .sharpeRatio(stored.getSharpeRatio())
                .runAt(stored.getRunAt())
                .build();
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "US 일봉 수집 (로보 백테스트용)", description = "선택 기간·로보 자산/벤치마크/섹터 ETF에 대해 yfinance 스크립트로 US 일봉 수집. startDate/endDate 미입력 시 최근 30일.")
    @PostMapping("/robo/collect-us-daily")
    public ResponseEntity<CollectUsDailyResponse> collectUsDaily(
            @RequestBody(required = false) CollectUsDailyRequest request) {
        LocalDate end = request != null && request.getEndDate() != null ? request.getEndDate() : LocalDate.now();
        LocalDate start = request != null && request.getStartDate() != null ? request.getStartDate()
                : end.minusDays(29);
        if (start.isAfter(end)) {
            start = end.minusDays(29);
        }
        List<String> symbols = roboSymbolsForCollection();
        int[] result = usMarketCollectionService.collectAndSaveRange(start, end, symbols);
        int collectedDays = result[0];
        int savedTotal = result[1];
        String message = null;
        if (collectedDays == 0 && savedTotal == 0) {
            message = "수집된 데이터가 없습니다. application.yml에 investment.data.us.yfinance-script-path가 설정되어 있고, Python/yfinance 스크립트가 동작하는지 확인하세요.";
        }
        return ResponseEntity.ok(CollectUsDailyResponse.builder()
                .collectedDays(collectedDays)
                .savedTotal(savedTotal)
                .message(message)
                .build());
    }

    /** 로보 백테스트에 필요한 US 심볼 목록 (자산 + 벤치마크 + 섹터 ETF, 중복 제거) */
    private List<String> roboSymbolsForCollection() {
        Set<String> set = new LinkedHashSet<>();
        set.addAll(roboBacktestProperties.getAssetSymbolList());
        set.addAll(roboBacktestProperties.getSectorEtfSymbolList());
        String bw = roboBacktestProperties.getBenchmarkWeights();
        if (bw != null && !bw.isBlank()) {
            for (String part : bw.split(",")) {
                String[] kv = part.trim().split("=");
                if (kv.length >= 1 && !kv[0].trim().isEmpty()) {
                    set.add(kv[0].trim());
                }
            }
        }
        String abs = roboBacktestProperties.getAbsoluteMomentumSymbol();
        if (abs != null && !abs.isBlank())
            set.add(abs);
        String rf = roboBacktestProperties.getRiskFreeSymbol();
        if (rf != null && !rf.isBlank())
            set.add(rf);
        return new ArrayList<>(set);
    }
}
