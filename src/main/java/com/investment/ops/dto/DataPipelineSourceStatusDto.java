package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 데이터 파이프라인 원천별 상태 (DART/SEC/KRX/US).
 * Ops 데이터 파이프라인 화면(/ops/data)용.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataPipelineSourceStatusDto {

    /** 원천 식별자 (DART, SEC, KRX, US) */
    private String sourceId;
    /** 표시명 */
    private String displayName;
    /** 마지막 배치 실행 시각 */
    private LocalDateTime lastRunTime;
    /** 최근 기준일 (뉴스: 최근 수집일, 시세: 최근 basDt) */
    private LocalDate lastBaselineDate;
    /** 상태: OK, WARNING, ERROR */
    private String status;
    /** 마지막 실패 시 오류 요약 (있을 때만) */
    private String errorSummary;
}
