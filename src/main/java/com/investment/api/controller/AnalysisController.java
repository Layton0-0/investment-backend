package com.investment.api.controller;

import com.investment.analysis.dto.AnalysisRequestDto;
import com.investment.analysis.dto.AnalysisResponseDto;
import com.investment.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * AI 분석 REST API
 */
@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {
    
    private final AnalysisService analysisService;
    
    @PostMapping
    public ResponseEntity<AnalysisResponseDto> analyze(
            @RequestBody @Valid AnalysisRequestDto request) {
        AnalysisResponseDto response = analysisService.analyze(request);
        return ResponseEntity.ok(response);
    }
}
