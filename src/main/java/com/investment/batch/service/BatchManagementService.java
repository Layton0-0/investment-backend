package com.investment.batch.service;

import com.investment.batch.dto.BatchJobDto;
import com.investment.batch.registry.BatchJobDefinition;
import com.investment.batch.registry.BatchJobRegistry;
import com.investment.batch.util.CronDescriptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 배치 관리 서비스.
 * 레지스트리에서 Job 목록을 가져오고, JobRepository 메타데이터(BATCH_* 테이블)에서 실행 횟수·마지막 실행 시각을 조회.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchManagementService {

    private static final String SQL_EXECUTION_COUNT = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION e INNER JOIN BATCH_JOB_INSTANCE i ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID WHERE i.JOB_NAME = ?";
    private static final String SQL_SUCCESS_COUNT = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION e INNER JOIN BATCH_JOB_INSTANCE i ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID WHERE i.JOB_NAME = ? AND e.STATUS = 'COMPLETED'";
    private static final String SQL_FAILURE_COUNT = "SELECT COUNT(*) FROM BATCH_JOB_EXECUTION e INNER JOIN BATCH_JOB_INSTANCE i ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID WHERE i.JOB_NAME = ? AND e.STATUS = 'FAILED'";
    private static final String SQL_LAST_END_TIME = "SELECT MAX(e.END_TIME) FROM BATCH_JOB_EXECUTION e INNER JOIN BATCH_JOB_INSTANCE i ON e.JOB_INSTANCE_ID = i.JOB_INSTANCE_ID WHERE i.JOB_NAME = ?";

    private final BatchJobRegistry batchJobRegistry;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 모든 배치 작업 목록 조회 (레지스트리 + JobRepository 집계).
     */
    public List<BatchJobDto> getAllBatchJobs() {
        List<BatchJobDto> jobs = new ArrayList<>();
        for (BatchJobDefinition def : batchJobRegistry.getDefinitions()) {
            BatchJobDto dto = BatchJobDto.builder()
                    .id(def.getId())
                    .name(def.getName())
                    .description(def.getDescription())
                    .cronExpression(def.getCronExpression())
                    .cronDescription(CronDescriptionUtil.toKoreanDescription(def.getCronExpression()))
                    .timeZone(def.getTimeZone())
                    .status("ACTIVE")
                    .triggerPath(def.getTriggerPath())
                    .executionCount(getExecutionCount(def.getId()))
                    .successCount(getSuccessCount(def.getId()))
                    .failureCount(getFailureCount(def.getId()))
                    .lastExecutionTime(getLastExecutionTime(def.getId()))
                    .build();
            try {
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of(def.getTimeZone()));
                dto.setNextExecutionTime(calculateNextExecution(def.getCronExpression(), now));
            } catch (Exception e) {
                log.warn("다음 실행 시간 계산 실패: jobId={}", def.getId(), e);
            }
            jobs.add(dto);
        }
        return jobs;
    }

    private Long getExecutionCount(String jobName) {
        try {
            Long v = jdbcTemplate.queryForObject(SQL_EXECUTION_COUNT, Long.class, jobName);
            return v != null ? v : 0L;
        } catch (Exception e) {
            log.trace("Execution count query failed for job={}: {}", jobName, e.getMessage());
            return 0L;
        }
    }

    private Long getSuccessCount(String jobName) {
        try {
            Long v = jdbcTemplate.queryForObject(SQL_SUCCESS_COUNT, Long.class, jobName);
            return v != null ? v : 0L;
        } catch (Exception e) {
            log.trace("Success count query failed for job={}: {}", jobName, e.getMessage());
            return 0L;
        }
    }

    private Long getFailureCount(String jobName) {
        try {
            Long v = jdbcTemplate.queryForObject(SQL_FAILURE_COUNT, Long.class, jobName);
            return v != null ? v : 0L;
        } catch (Exception e) {
            log.trace("Failure count query failed for job={}: {}", jobName, e.getMessage());
            return 0L;
        }
    }

    private LocalDateTime getLastExecutionTime(String jobName) {
        try {
            var row = jdbcTemplate.queryForObject(SQL_LAST_END_TIME, java.sql.Timestamp.class, jobName);
            return row != null ? row.toLocalDateTime() : null;
        } catch (Exception e) {
            log.trace("Last execution time query failed for job={}: {}", jobName, e.getMessage());
            return null;
        }
    }

    /**
     * 다음 실행 시간 계산 (간단한 버전).
     */
    private LocalDateTime calculateNextExecution(String cronExpression, ZonedDateTime now) {
        try {
            if ("0 0 9 * * *".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(9).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now))
                    next = next.plusDays(1);
                return next.toLocalDateTime();
            }
            if ("0 0 * * * *".equals(cronExpression)) {
                return now.plusHours(1).withMinute(0).withSecond(0).toLocalDateTime();
            }
            if ("0 0 8 * * *".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(8).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now))
                    next = next.plusDays(1);
                return next.toLocalDateTime();
            }
            if (cronExpression != null && cronExpression.startsWith("0 */10")) {
                int minute = now.getMinute();
                int nextMin = ((minute / 10) + 1) * 10;
                if (nextMin >= 60)
                    return now.plusHours(1).withMinute(0).withSecond(0).toLocalDateTime();
                return now.withMinute(nextMin).withSecond(0).toLocalDateTime();
            }
            if ("0 0 9 * * MON".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(9).withMinute(0).withSecond(0);
                while (next.getDayOfWeek().getValue() != 1 || next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            if (cronExpression != null && cronExpression.startsWith("0 */15")) {
                int minute = now.getMinute();
                int nextMin = ((minute / 15) + 1) * 15;
                if (nextMin >= 60)
                    return now.plusHours(1).withMinute(0).withSecond(0).toLocalDateTime();
                return now.withMinute(nextMin).withSecond(0).toLocalDateTime();
            }
            if ("0 0 16 * * *".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(16).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now))
                    next = next.plusDays(1);
                return next.toLocalDateTime();
            }
            if ("0 0 17 * * *".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(17).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now))
                    next = next.plusDays(1);
                return next.toLocalDateTime();
            }
            if ("0 10 9 * * *".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(9).withMinute(10).withSecond(0);
                if (next.isBefore(now) || next.equals(now))
                    next = next.plusDays(1);
                return next.toLocalDateTime();
            }
            if (cronExpression != null && cronExpression.contains("*/5") && cronExpression.contains("9-15")) {
                int minute = now.getMinute();
                int nextMin = ((minute / 5) + 1) * 5;
                ZonedDateTime next = nextMin >= 60 ? now.plusHours(1).withMinute(0) : now.withMinute(nextMin);
                next = next.withSecond(0);
                if (next.getHour() < 9)
                    next = next.withHour(9).withMinute(0);
                if (next.getHour() > 15)
                    next = next.plusDays(1).withHour(9).withMinute(0);
                if (next.getDayOfWeek().getValue() >= 6) {
                    next = next.plusDays(8 - next.getDayOfWeek().getValue()).withHour(9).withMinute(0);
                }
                return next.toLocalDateTime();
            }
            if ("0 * * * * *".equals(cronExpression)) {
                return now.plusMinutes(1).withSecond(0).toLocalDateTime();
            }
            if ("0 30 8 1 * *".equals(cronExpression)) {
                ZonedDateTime next = now.withDayOfMonth(1).withHour(8).withMinute(30).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = now.plusMonths(1).withDayOfMonth(1).withHour(8).withMinute(30).withSecond(0);
                }
                return next.toLocalDateTime();
            }
            if ("0 0 9 L * *".equals(cronExpression)) {
                ZonedDateTime next = now.withDayOfMonth(now.toLocalDate().lengthOfMonth()).withHour(9).withMinute(0)
                        .withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = now.plusMonths(1).withDayOfMonth(now.plusMonths(1).toLocalDate().lengthOfMonth()).withHour(9)
                            .withMinute(0).withSecond(0);
                }
                return next.toLocalDateTime();
            }
            if ("0 5 16 * * MON-FRI".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(16).withMinute(5).withSecond(0);
                if (next.getDayOfWeek().getValue() >= 6)
                    next = next.plusDays(8 - next.getDayOfWeek().getValue());
                if (next.isBefore(now) || next.equals(now))
                    next = next.plusDays(1);
                while (next.getDayOfWeek().getValue() >= 6)
                    next = next.plusDays(1);
                return next.toLocalDateTime();
            }
            if ("0 10,40 9 * * MON-FRI".equals(cronExpression)) {
                ZonedDateTime next = now.withHour(9).withMinute(10).withSecond(0);
                if (next.isBefore(now))
                    next = next.withMinute(40);
                if (next.isBefore(now))
                    next = next.plusDays(1).withMinute(10);
                if (next.getDayOfWeek().getValue() >= 6)
                    next = next.plusDays(8 - next.getDayOfWeek().getValue()).withHour(9).withMinute(10);
                return next.toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("다음 실행 시간 계산 실패: cron={}", cronExpression, e);
        }
        return now.plusHours(1).toLocalDateTime();
    }
}
