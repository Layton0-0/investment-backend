package com.investment.api.controller;

import com.investment.batch.dto.BatchJobDto;
import com.investment.batch.service.BatchManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 배치 작업 목록 REST API.
 * 스케줄 현황(데이터 파이프라인 등)에서 사용. nginx가 /api 만 백엔드로 전달하므로
 * /api/v1/batch/jobs 로 노출하여 프록시 없이 동작하도록 함.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "배치", description = "배치 작업 목록 조회 API")
public class BatchController {

    private final BatchManagementService batchManagementService;

    @GetMapping("/jobs")
    @Operation(summary = "배치 작업 목록", description = "스케줄러로 실행되는 배치 작업 목록을 조회합니다. 인증 필요.")
    public ResponseEntity<List<BatchJobDto>> getJobs() {
        log.debug("배치 작업 목록 조회");
        List<BatchJobDto> jobs = batchManagementService.getAllBatchJobs();
        return ResponseEntity.ok(jobs);
    }
}
