package com.investment.api.controller;

import com.investment.ops.dto.AuditLogListResponseDto;
import com.investment.ops.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Ops 트레이드 저널: 매매 결정(매수/매도/스킵) 내역 조회 (ADMIN 전용). P6-3.
 */
@Tag(name = "Ops 트레이드 저널", description = "매매 결정 사유 조회 (ADMIN 전용)")
@RestController
@RequestMapping("/api/v1/ops")
@RequiredArgsConstructor
public class OpsTradeJournalController {

    private final AuditLogService auditLogService;

    @Operation(summary = "트레이드 저널 목록", description = "최근 매매 결정(BUY/SELL/SKIP/DRY_RUN) 내역을 페이징 조회합니다.")
    @GetMapping("/trade-journal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuditLogListResponseDto> getTradeJournal(
            @Parameter(description = "페이지 (0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (1~100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "기간 시작 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "기간 종료 (yyyy-MM-dd)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant() : null;
        Instant toInstant = to != null ? ZonedDateTime.of(to.atTime(23, 59, 59, 999_999_999), ZoneId.of("Asia/Seoul")).toInstant() : null;
        AuditLogListResponseDto dto = auditLogService.findPage(page, size,
                AuditLogService.EVENT_TRADE_DECISION, fromInstant, toInstant);
        return ResponseEntity.ok(dto);
    }
}
