package com.investment.api.controller;

import com.investment.backtest.BacktestService;
import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Backtest", description = "백테스트 API")
@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestService backtestService;

    @Operation(summary = "백테스트 실행", description = "기간·시장·전략타입·초기자본으로 4단계 파이프라인 재생 후 메트릭·수익곡선·거래 목록 반환")
    @PostMapping
    public ResponseEntity<BacktestRunResult> run(@RequestBody @Valid BacktestRunRequest request) {
        BacktestRunResult result = backtestService.run(request);
        return ResponseEntity.ok(result);
    }
}
