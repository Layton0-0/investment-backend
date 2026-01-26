package com.investment.api.controller;

import com.investment.strategy.domain.StrategyType;
import com.investment.strategy.dto.StrategyDto;
import com.investment.strategy.dto.StrategyStatusUpdateDto;
import com.investment.strategy.service.StrategyManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 전략 REST API
 */
@RestController
@RequestMapping("/api/v1/strategies")
@RequiredArgsConstructor
public class StrategyApiController {
    
    private final StrategyManagementService strategyManagementService;
    
    @GetMapping("/{accountNo}")
    public ResponseEntity<List<StrategyDto>> getStrategies(
            @PathVariable @NotBlank String accountNo) {
        List<StrategyDto> strategies = strategyManagementService.getStrategies(accountNo);
        return ResponseEntity.ok(strategies);
    }
    
    @GetMapping("/{accountNo}/{strategyType}")
    public ResponseEntity<StrategyDto> getStrategy(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType) {
        StrategyDto strategy = strategyManagementService.getStrategy(accountNo, strategyType);
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
            @RequestBody @Valid StrategyStatusUpdateDto dto) {
        StrategyDto strategy = strategyManagementService.updateStrategyStatus(accountNo, strategyType, dto);
        return ResponseEntity.ok(strategy);
    }
    
    @PostMapping("/{accountNo}/{strategyType}/activate")
    public ResponseEntity<StrategyDto> activateStrategy(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType) {
        StrategyDto strategy = strategyManagementService.activateStrategy(accountNo, strategyType);
        return ResponseEntity.ok(strategy);
    }
    
    @PostMapping("/{accountNo}/{strategyType}/stop")
    public ResponseEntity<StrategyDto> stopStrategy(
            @PathVariable @NotBlank String accountNo,
            @PathVariable StrategyType strategyType) {
        StrategyDto strategy = strategyManagementService.stopStrategy(accountNo, strategyType);
        return ResponseEntity.ok(strategy);
    }
}
