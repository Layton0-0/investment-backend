package com.investment.api.controller;

import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.dto.StrategyStatusUpdateDto;
import com.investment.strategy.service.StrategyManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Tag(name = "Strategy", description = "전략 API (시장 KR/US 지원)")
@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyApiController {

    private final StrategyManagementService strategyManagementService;

    @GetMapping("/{accountNo}")
    public ResponseEntity<List<StrategyDto>> getStrategies(
            @PathVariable @NotBlank String accountNo,
            @RequestParam(required = false) String market) {
        List<StrategyDto> strategies = strategyManagementService.getStrategies(accountNo, market);
        return ResponseEntity.ok(strategies);
    }

    @Operation(summary = "전략 상세 조회", description = "계좌·시장·전략타입별 전략. market 미지정 시 KR.")
    @GetMapping("/{accountNo}/{strategyType}")
    public ResponseEntity<StrategyDto> getStrategy(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType,
            @Parameter(description = "시장 (KR, US). 선택") @RequestParam(required = false) String market) {
        StrategyDto strategy = strategyManagementService.getStrategy(accountNo, market, strategyType);
        return ResponseEntity.ok(strategy);
    }

    @PostMapping
    public ResponseEntity<StrategyDto> createOrUpdateStrategy(
            @RequestBody @Valid StrategyDto dto) {
        StrategyDto strategy = strategyManagementService.saveStrategy(dto);
        return ResponseEntity.ok(strategy);
    }

    @PutMapping("/{accountNo}/{strategyType}/status")
    public ResponseEntity<StrategyDto> updateStrategyStatus(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType,
            @RequestParam(required = false) String market,
            @RequestBody @Valid StrategyStatusUpdateDto dto) {
        StrategyDto strategy = strategyManagementService.updateStrategyStatus(accountNo, market, strategyType, dto);
        return ResponseEntity.ok(strategy);
    }

    @PostMapping("/{accountNo}/{strategyType}/activate")
    public ResponseEntity<StrategyDto> activateStrategy(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType,
            @RequestParam(required = false) String market) {
        StrategyDto strategy = strategyManagementService.activateStrategy(accountNo, market, strategyType);
        return ResponseEntity.ok(strategy);
    }

    @PostMapping("/{accountNo}/{strategyType}/stop")
    public ResponseEntity<StrategyDto> stopStrategy(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType,
            @RequestParam(required = false) String market) {
        StrategyDto strategy = strategyManagementService.stopStrategy(accountNo, market, strategyType);
        return ResponseEntity.ok(strategy);
    }
}
