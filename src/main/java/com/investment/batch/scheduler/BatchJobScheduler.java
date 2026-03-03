package com.investment.batch.scheduler;

import com.investment.alert.EmergencyAlertService;
import com.investment.batch.registry.BatchJobDefinition;
import com.investment.batch.registry.BatchJobRegistry;
import com.investment.setting.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.ZoneId;

/**
 * 레지스트리 cron에 따라 각 Job을 주기적으로 실행.
 * 기동 시 모든 Job 정의에 대해 TaskScheduler에 등록.
 * Job 실패 알림 여부는 Admin 시스템 설정(batch.failureAlertEnabled)에서 조회.
 */
@Slf4j
@Component
public class BatchJobScheduler {

    private static final String BATCH_TABLE_MISSING_HINT = "Spring Batch 메타데이터 테이블이 없습니다. docs/08-setup-guides/01-local-setup-complete.md §문제 해결 - Spring Batch 메타데이터 테이블을 참고해 V20 마이그레이션을 적용하세요.";

    private static volatile boolean batchTableMissingLogged;

    private final BatchJobRegistry batchJobRegistry;
    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;
    private final TaskScheduler taskScheduler;
    private final SystemSettingService systemSettingService;

    @Autowired(required = false)
    private EmergencyAlertService emergencyAlertService;

    public BatchJobScheduler(BatchJobRegistry batchJobRegistry, JobLauncher jobLauncher,
                             ApplicationContext applicationContext, TaskScheduler taskScheduler,
                             SystemSettingService systemSettingService) {
        this.batchJobRegistry = batchJobRegistry;
        this.jobLauncher = jobLauncher;
        this.applicationContext = applicationContext;
        this.taskScheduler = taskScheduler;
        this.systemSettingService = systemSettingService;
    }

    @PostConstruct
    public void scheduleJobs() {
        for (BatchJobDefinition def : batchJobRegistry.getDefinitions()) {
            String cron = def.getCronExpression();
            if (cron == null || cron.isBlank()) {
                log.debug("Skipping schedule for manual-only job: id={}", def.getId());
                continue;
            }
            try {
                Job job = applicationContext.getBean(def.getId(), Job.class);
                CronTrigger trigger = new CronTrigger(cron, ZoneId.of(def.getTimeZone()));
                String jobId = def.getId();
                taskScheduler.schedule(() -> runJob(jobId, job), trigger);
                log.debug("Scheduled batch job: id={}, cron={}", jobId, cron);
            } catch (Exception e) {
                log.warn("Failed to schedule batch job: id={}", def.getId(), e);
            }
        }
    }

    private void runJob(String jobId, Job job) {
        try {
            jobLauncher.run(job, new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis())
                    .toJobParameters());
        } catch (Exception e) {
            if (isBatchTableMissing(e) && !batchTableMissingLogged) {
                batchTableMissingLogged = true;
                log.warn("Batch job skipped (metadata tables missing): jobId={}. {}", jobId, BATCH_TABLE_MISSING_HINT);
            } else if (!isBatchTableMissing(e)) {
                log.error("Batch job execution failed: jobId={}", jobId, e);
                boolean failureAlertEnabled = Boolean.TRUE.equals(systemSettingService.getBoolean("batch.failureAlertEnabled"));
                if (failureAlertEnabled && emergencyAlertService != null) {
                    String message = "배치 Job 실패: jobId=" + jobId + ", error=" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    emergencyAlertService.sendRiskEventAlert("ERROR", "BatchJob", message);
                }
            }
        }
    }

    private static boolean isBatchTableMissing(Throwable t) {
        if (t == null) {
            return false;
        }
        String msg = t.getMessage();
        if (msg != null && msg.contains("BATCH_JOB") && msg.contains("doesn't exist")) {
            return true;
        }
        Throwable cause = t.getCause();
        return cause != null && cause != t && isBatchTableMissing(cause);
    }
}
