package com.investment.api.controller;

import com.investment.report.dto.TaxReportSummaryDto;
import com.investment.report.service.TaxReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

/**
 * 연말 세금·리포트 API.
 * 기획요청 §9: 연간 실현손익·국내/해외·배당·예상 세금. 집계는 기간별손익 API 기반.
 */
@Tag(name = "연말 세금·리포트", description = "연말 세금 요약 (인증 필요)")
@RestController
@RequestMapping("/api/v1/report/tax")
@RequiredArgsConstructor
public class TaxReportController {

    private final TaxReportService taxReportService;

    @Operation(summary = "세금 요약", description = "기준 연도 연말 세금 요약. 실데이터는 기간별손익조회 기반 집계.")
    @GetMapping("/summary")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TaxReportSummaryDto> getSummary(
            Principal principal,
            @Parameter(description = "기준 연도. 미입력 시 현재 연도") @RequestParam(required = false) Integer year) {
        String userId = principal != null ? principal.getName() : null;
        TaxReportSummaryDto dto = taxReportService.getSummary(userId, year);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "세금 요약 내보내기", description = "기준 연도 요약을 CSV 또는 PDF로 다운로드.")
    @GetMapping(value = "/summary/export", produces = { MediaType.APPLICATION_OCTET_STREAM_VALUE, "text/csv" })
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportSummary(
            Principal principal,
            @Parameter(description = "기준 연도") @RequestParam(required = false) Integer year,
            @Parameter(description = "format: csv 또는 pdf") @RequestParam(defaultValue = "csv") String format) {
        String userId = principal != null ? principal.getName() : null;
        TaxReportSummaryDto dto = taxReportService.getSummary(userId, year);
        byte[] body;
        String filename;
        MediaType mediaType;
        if ("pdf".equalsIgnoreCase(format)) {
            body = taxReportService.exportSummaryAsPdf(dto);
            filename = "tax-summary-" + dto.getYear() + ".pdf";
            mediaType = MediaType.APPLICATION_PDF;
        } else {
            body = taxReportService.exportSummaryAsCsv(dto);
            filename = "tax-summary-" + dto.getYear() + ".csv";
            mediaType = new MediaType("text", "csv", StandardCharsets.UTF_8);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .contentLength(body.length)
                .body(body);
    }
}
