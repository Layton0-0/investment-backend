package com.investment.api.controller;

import com.investment.factor.dto.PipelineSummaryDto;
import com.investment.factor.service.PipelineSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Pipeline", description = "자동투자 파이프라인 요약 API")
@RestController
@RequestMapping("/api/v1/pipeline")
@RequiredArgsConstructor
public class PipelineController {

    private final PipelineSummaryService pipelineSummaryService;

    @Operation(summary = "파이프라인 요약", description = "기준일/계좌에 대한 유니버스·시그널(KR/US)·자금배분·보유 포지션 요약을 반환합니다.")
    @GetMapping("/summary")
    public ResponseEntity<PipelineSummaryDto> getSummary(
            @Parameter(description = "계좌번호(필수)") @RequestParam String accountNo,
            @Parameter(description = "기준일(yyyy-MM-dd). 미입력 시 전일(자동투자 실행 기준일과 맞춤)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate basDt) {

        LocalDate targetDate = basDt != null ? basDt : LocalDate.now().minusDays(1);
        PipelineSummaryDto dto = pipelineSummaryService.getSummary(targetDate, accountNo);
        return ResponseEntity.ok(dto);
    }
}
