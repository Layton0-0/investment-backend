package com.investment.analysis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * AI 분석 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisRequestDto {
    
    @NotBlank(message = "종목코드는 필수입니다")
    private String symbol;
    
    @NotNull(message = "분석기간은 필수입니다")
    private Integer periodDays;
    
    private List<String> indicators; // 분석 지표 목록
}
