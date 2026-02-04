package com.investment.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 배치 작업 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobDto {

    private String id;
    private String name;
    private String description;
    private String cronExpression;
    /** cron을 한국어로 설명한 문자열 (예: 10분마다). 표시용. */
    private String cronDescription;
    private String timeZone;
    private String status; // ACTIVE, PAUSED, DISABLED
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private String lastExecutionResult;
    private Long executionCount;
    private Long successCount;
    private Long failureCount;
    /** 수동 트리거 API 경로 (POST). 있으면 "지금 실행" 버튼 노출 */
    private String triggerPath;
}
