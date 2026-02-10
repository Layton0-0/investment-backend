package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터 파이프라인 전체 상태 응답.
 * Ops 데이터 파이프라인 화면(/ops/data)용.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DataPipelineStatusDto {

    /** 원천별 상태 목록 (DART, SEC, KRX, US) */
    private List<DataPipelineSourceStatusDto> sources;
    /** 응답 생성 시각 */
    private LocalDateTime updatedAt;
}
