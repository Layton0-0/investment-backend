package com.investment.batch.controller;

import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.service.AuthService;
import com.investment.batch.dto.BatchJobDto;
import com.investment.batch.service.BatchManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * 스케줄 현황(배치 관리) 컨트롤러
 */
@Slf4j
@Controller
@RequestMapping("/batch")
@RequiredArgsConstructor
public class BatchManagementController {

    private final BatchManagementService batchManagementService;
    private final AuthService authService;

    /**
     * 스케줄 현황 페이지
     */
    @GetMapping
    public String batchManagement(Authentication authentication, Model model) {
        log.debug("스케줄 현황 페이지 조회");
        if (authentication != null && authentication.isAuthenticated()) {
            try {
                MyPageResponseDto userInfo = authService.getMyPage(authentication.getName());
                model.addAttribute("userInfo", userInfo);
            } catch (Exception e) {
                log.warn("사용자 정보 조회 실패: {}", e.getMessage());
            }
        }
        List<BatchJobDto> jobs = batchManagementService.getAllBatchJobs();
        model.addAttribute("jobs", jobs);
        return "batch-management";
    }
    
    /**
     * 배치 작업 목록 API
     */
    @GetMapping("/api/jobs")
    public ResponseEntity<List<BatchJobDto>> getBatchJobs() {
        log.debug("배치 작업 목록 조회");
        List<BatchJobDto> jobs = batchManagementService.getAllBatchJobs();
        return ResponseEntity.ok(jobs);
    }
}
