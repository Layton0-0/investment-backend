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
    private String timeZone;
    private String status; // ACTIVE, PAUSED, DISABLED
    private LocalDateTime lastExecutionTime;
    private LocalDateTime nextExecutionTime;
    private String lastExecutionResult;
    private Long executionCount;
    private Long successCount;
    private Long failureCount;
}
