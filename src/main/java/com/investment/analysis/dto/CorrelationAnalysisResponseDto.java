package com.investment.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 상관관계 분석 API 응답 DTO.
 * 포트폴리오(또는 종목 목록) 내 종목 간 수익률 상관계수 행렬.
 * matrix[i][j] = symbols[i] vs symbols[j] 상관계수 (-1 ~ 1). 대각선은 1.
 */
@Schema(description = "상관관계 분석 결과 (수익률 기반 Pearson 상관계수 행렬)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CorrelationAnalysisResponseDto {

    @Schema(description = "기준 시장 (KR/US)")
    private String market;

    @Schema(description = "분석 대상 종목 목록 (행렬 행/열 순서와 동일)")
    private List<String> symbols;

    @Schema(description = "분석 기간 시작일")
    private LocalDate fromDate;

    @Schema(description = "분석 기간 종료일")
    private LocalDate toDate;

    @Schema(description = "상관계수 행렬. matrix[i][j] = symbols[i] vs symbols[j]. -1~1, 대각선 1. 데이터 부족 시 빈 리스트")
    private List<List<Double>> matrix;
}
