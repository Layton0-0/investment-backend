package com.investment.api.controller;

import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.ops.dto.ReconciliationResultDto;
import com.investment.ops.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Ops 포지션 정합성(Reconciliation) 리포트 API (ADMIN 전용).
 * 브로커 실잔고 vs DB(TB_STRATEGY_POSITION) 비교 결과를 반환.
 */
@Tag(name = "Ops 정합성", description = "포지션 정합성(Re-sync) 리포트 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops/reconcile")
@RequiredArgsConstructor
public class OpsReconcileController {

    private final TradingSettingRepository tradingSettingRepository;
    private final ReconciliationService reconciliationService;

    @Operation(summary = "정합성 리포트", description = "계좌별 브로커-DB 포지션 비교. accountNo 미지정 시 자동투자 ON 전체 계좌.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReconcileReportResponse> getReconcileReport(
            @Parameter(description = "계좌번호 (미입력 시 전체)") @RequestParam(required = false) String accountNo) {
        List<ReconciliationResultDto> results = new ArrayList<>();
        if (accountNo != null && !accountNo.isBlank()) {
            tradingSettingRepository.findByAccountNo(accountNo.trim())
                    .filter(s -> s.getUserId() != null)
                    .ifPresent(s -> results.add(reconciliationService.reconcile(s.getUserId(), s.getAccountNo())));
        } else {
            List<TradingSetting> settings = tradingSettingRepository.findAllByAutoTradingEnabledTrue();
            if (settings != null) {
                for (TradingSetting s : settings) {
                    if (s.getUserId() != null && s.getAccountNo() != null && !s.getAccountNo().isBlank()) {
                        results.add(reconciliationService.reconcile(s.getUserId(), s.getAccountNo()));
                    }
                }
            }
        }
        return ResponseEntity.ok(new ReconcileReportResponse(results));
    }

    public static class ReconcileReportResponse {
        private final List<ReconciliationResultDto> accounts;

        public ReconcileReportResponse(List<ReconciliationResultDto> accounts) {
            this.accounts = accounts != null ? accounts : List.of();
        }

        public List<ReconciliationResultDto> getAccounts() {
            return accounts;
        }
    }
}
