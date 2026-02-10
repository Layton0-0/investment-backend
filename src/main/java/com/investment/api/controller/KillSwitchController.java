package com.investment.api.controller;

import com.investment.risk.service.TradingHaltService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kill Switch API: 전체 주문 차단 상태 조회·설정.
 * Phase 2 기관급 리스크 레이어.
 */
@Tag(name = "Kill Switch", description = "긴급 시 전체 주문 차단 상태")
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
public class KillSwitchController {

    private final TradingHaltService tradingHaltService;

    @Operation(summary = "Kill Switch 상태 조회")
    @GetMapping("/kill-switch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> getKillSwitchStatus() {
        boolean halt = tradingHaltService.isHaltAllOrders();
        return ResponseEntity.ok(Map.of("haltAllOrders", halt));
    }

    @Operation(summary = "Kill Switch 설정 (차단/해제)")
    @PutMapping("/kill-switch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Boolean>> setKillSwitch(@RequestBody Map<String, Boolean> body) {
        Boolean halt = body.get("haltAllOrders");
        if (halt == null) {
            halt = false;
        }
        tradingHaltService.setHaltAllOrders(halt);
        return ResponseEntity.ok(Map.of("haltAllOrders", tradingHaltService.isHaltAllOrders()));
    }
}
