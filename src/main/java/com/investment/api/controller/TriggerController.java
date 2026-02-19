package com.investment.api.controller;

import com.investment.batch.registry.BatchJobRegistry;
import com.investment.ops.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 스케줄 작업 수동 트리거 API.
 * Spring Batch Job을 JobLauncher로 실행하며, 스케줄 현황 화면 "지금 실행"에서 호출.
 */
@Tag(name = "Trigger", description = "스케줄 작업 수동 트리거 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/trigger")
@RequiredArgsConstructor
public class TriggerController {

    private static final String TRIGGER_PATH_PREFIX = "/api/v1/trigger";

    private final BatchJobRegistry batchJobRegistry;
    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;
    private final AuditLogService auditLogService;

    private void recordManualTrigger(Principal principal, String pathSuffix, ResponseEntity<Map<String, Object>> response) {
        try {
            boolean success = response.getBody() != null && Boolean.TRUE.equals(response.getBody().get("success"));
            String userId = principal != null ? principal.getName() : null;
            auditLogService.record(AuditLogService.EVENT_MANUAL_TRIGGER, userId, null,
                    "trigger path=" + pathSuffix, success ? AuditLogService.RESULT_SUCCESS : AuditLogService.RESULT_FAILURE, null);
        } catch (Exception e) {
            log.trace("감사 로그 기록 스킵: {}", e.getMessage());
        }
    }

    private ResponseEntity<Map<String, Object>> runTrigger(String pathSuffix, String successMessage,
            String failureMessage,
            JobParametersBuilder paramsBuilder) {
        String triggerPath = TRIGGER_PATH_PREFIX + pathSuffix;
        String jobId = batchJobRegistry.getJobIdByTriggerPath(triggerPath);
        if (jobId == null) {
            log.warn("No job registered for trigger path: {}", triggerPath);
            return ResponseEntity.ok(Map.of("success", false, "message", failureMessage + " (job not found)"));
        }
        paramsBuilder.addLong("run.id", System.currentTimeMillis());
        try {
            Job job = applicationContext.getBean(jobId, Job.class);
            JobExecution execution = jobLauncher.run(job, paramsBuilder.toJobParameters());
            boolean success = execution.getStatus() == BatchStatus.COMPLETED;
            String message = success ? successMessage
                    : (execution.getExitStatus().getExitDescription() != null
                            ? execution.getExitStatus().getExitDescription()
                            : failureMessage);
            return ResponseEntity.ok(Map.of("success", success, "message", message));
        } catch (Exception e) {
            log.warn("Trigger failed: jobId={}, error={}", jobId, e.getMessage());
            return ResponseEntity.ok(Map.of("success", false, "message", failureMessage + ": " + e.getMessage()));
        }
    }

    // DART/SEC 공시 수집은 Python investment-data-collector (POST /dart-collect, /sec-collect)에서 수행

    @Operation(summary = "KRX 일별 시세 수집", description = "KRX 일별 시세 수집을 즉시 실행. basDt 미입력 시 오늘")
    @PostMapping("/krx-daily")
    public ResponseEntity<Map<String, Object>> triggerKrxDaily(
            Principal principal,
            @Parameter(description = "기준일 (yyyy-MM-dd)") @RequestParam(required = false) LocalDate basDt) {
        LocalDate target = basDt != null ? basDt : LocalDate.now();
        JobParametersBuilder params = new JobParametersBuilder();
        params.addString("basDt", target.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/krx-daily", "KRX 일별 수집 완료", "KRX 일별 수집 실패", params);
        recordManualTrigger(principal, "/krx-daily", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity.ok(Map.of("success", true, "message", "KRX 일별 수집 완료", "basDt", target.toString()));
        }
        return result;
    }

    @Operation(summary = "US 일별 시세 수집", description = "US 시장 일별 시세 수집을 즉시 실행. basDt 미입력 시 오늘")
    @PostMapping("/us-daily")
    public ResponseEntity<Map<String, Object>> triggerUsDaily(
            Principal principal,
            @Parameter(description = "기준일 (yyyy-MM-dd)") @RequestParam(required = false) LocalDate basDt) {
        LocalDate target = basDt != null ? basDt : LocalDate.now();
        JobParametersBuilder params = new JobParametersBuilder();
        params.addString("basDt", target.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/us-daily", "US 일별 수집 완료", "US 일별 수집 실패", params);
        recordManualTrigger(principal, "/us-daily", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity.ok(Map.of("success", true, "message", "US 일별 수집 완료", "basDt", target.toString()));
        }
        return result;
    }

    @Operation(summary = "KRX 일별 시세 백필", description = "과거 기간 KRX 일별 시세 수집(스트레스 구간 등). from·to 필수.")
    @PostMapping("/krx-daily-backfill")
    public ResponseEntity<Map<String, Object>> triggerKrxDailyBackfill(
            Principal principal,
            @Parameter(description = "시작일 (yyyy-MM-dd)", required = true) @RequestParam LocalDate from,
            @Parameter(description = "종료일 (yyyy-MM-dd)", required = true) @RequestParam LocalDate to) {
        JobParametersBuilder params = new JobParametersBuilder();
        params.addString("fromDate", from.toString());
        params.addString("toDate", to.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/krx-daily-backfill", "KRX 백필 완료", "KRX 백필 실패", params);
        recordManualTrigger(principal, "/krx-daily-backfill", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity.ok(Map.of("success", true, "message", "KRX 백필 완료", "from", from.toString(), "to", to.toString()));
        }
        return result;
    }

    @Operation(summary = "US 일별 시세 백필", description = "과거 기간 US 일별 시세 수집(스트레스 구간 등). from·to 필수.")
    @PostMapping("/us-daily-backfill")
    public ResponseEntity<Map<String, Object>> triggerUsDailyBackfill(
            Principal principal,
            @Parameter(description = "시작일 (yyyy-MM-dd)", required = true) @RequestParam LocalDate from,
            @Parameter(description = "종료일 (yyyy-MM-dd)", required = true) @RequestParam LocalDate to) {
        JobParametersBuilder params = new JobParametersBuilder();
        params.addString("fromDate", from.toString());
        params.addString("toDate", to.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/us-daily-backfill", "US 백필 완료", "US 백필 실패", params);
        recordManualTrigger(principal, "/us-daily-backfill", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity.ok(Map.of("success", true, "message", "US 백필 완료", "from", from.toString(), "to", to.toString()));
        }
        return result;
    }

    @Operation(summary = "팩터 계산", description = "유니버스 필터 및 팩터(시그널) 계산을 즉시 실행")
    @PostMapping("/factor-calculation")
    public ResponseEntity<Map<String, Object>> triggerFactorCalculation(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/factor-calculation", "팩터 계산 완료", "팩터 계산 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/factor-calculation", result);
        return result;
    }

    @Operation(summary = "자동매수(통합)", description = "공통 전처리 → 로보(ETF) → 파이프라인(개별종목) 순으로 통합 실행. dryRun=true면 실제 주문 없음")
    @PostMapping("/auto-buy")
    public ResponseEntity<Map<String, Object>> triggerAutoBuy(
            Principal principal,
            @Parameter(description = "true면 실제 주문 없이 실행") @RequestParam(required = false) Boolean dryRun) {
        JobParametersBuilder params = new JobParametersBuilder();
        if (dryRun != null)
            params.addString("dryRun", dryRun.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/auto-buy", "자동매수(통합) 완료", "자동매수(통합) 실패", params);
        recordManualTrigger(principal, "/auto-buy", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "자동매수(통합) 완료", "dryRun", Boolean.TRUE.equals(dryRun)));
        }
        return result;
    }

    @Operation(summary = "파이프라인 실행", description = "4단계 파이프라인 실행. dryRun=true면 주문 미실행")
    @PostMapping("/pipeline-execution")
    public ResponseEntity<Map<String, Object>> triggerPipelineExecution(
            Principal principal,
            @Parameter(description = "true면 실제 주문 없이 실행") @RequestParam(required = false) Boolean dryRun) {
        JobParametersBuilder params = new JobParametersBuilder();
        if (dryRun != null)
            params.addString("dryRun", dryRun.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/pipeline-execution", "파이프라인 실행 완료", "파이프라인 실행 실패",
                params);
        recordManualTrigger(principal, "/pipeline-execution", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "파이프라인 실행 완료", "dryRun", Boolean.TRUE.equals(dryRun)));
        }
        return result;
    }

    @Operation(summary = "파이프라인 청산 평가", description = "보유 포지션 청산 규칙 평가 및 매도 시그널 시 주문 실행")
    @PostMapping("/pipeline-exit")
    public ResponseEntity<Map<String, Object>> triggerPipelineExit(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/pipeline-exit", "파이프라인 청산 평가 완료", "파이프라인 청산 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/pipeline-exit", result);
        return result;
    }

    @Operation(summary = "체결 확인 후 포지션 등록", description = "체결된 주문에 대해 포지션 등록")
    @PostMapping("/fill-confirmation")
    public ResponseEntity<Map<String, Object>> triggerFillConfirmation(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/fill-confirmation", "체결 확인 완료", "체결 확인 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/fill-confirmation", result);
        return result;
    }

    @Operation(summary = "미체결 확인", description = "PENDING N분 경과 주문에 대해 Discord 긴급 알림")
    @PostMapping("/unfilled-check")
    public ResponseEntity<Map<String, Object>> triggerUnfilledCheck(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/unfilled-check", "미체결 확인 완료", "미체결 확인 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/unfilled-check", result);
        return result;
    }

    @Operation(summary = "리스크 이벤트 알림", description = "일일 손실 한도 임박·VaR 95% 초과 검사 후 Discord 알림 발송")
    @PostMapping("/risk-event-alert")
    public ResponseEntity<Map<String, Object>> triggerRiskEventAlert(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/risk-event-alert", "리스크 이벤트 알림 검사 완료", "리스크 이벤트 알림 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/risk-event-alert", result);
        return result;
    }

    @Operation(summary = "로보 리밸런싱", description = "로보 어드바이저 리밸런싱. dryRun=true면 백테스트만 실행·저장")
    @PostMapping("/robo-rebalance")
    public ResponseEntity<Map<String, Object>> triggerRoboRebalance(
            Principal principal,
            @Parameter(description = "true면 실제 ETF 주문 없이 백테스트만 실행") @RequestParam(required = false) Boolean dryRun) {
        JobParametersBuilder params = new JobParametersBuilder();
        if (dryRun != null)
            params.addString("dryRun", dryRun.toString());
        ResponseEntity<Map<String, Object>> result = runTrigger("/robo-rebalance", "로보 리밸런싱 완료", "로보 리밸런싱 실패", params);
        recordManualTrigger(principal, "/robo-rebalance", result);
        if (result.getBody() != null && result.getBody().get("success") == Boolean.TRUE) {
            return ResponseEntity
                    .ok(Map.of("success", true, "message", "로보 리밸런싱 완료", "dryRun", Boolean.TRUE.equals(dryRun)));
        }
        return result;
    }

    @Operation(summary = "일일 PnL 기록", description = "장 마감 후 계좌별 당일 수익률 기록")
    @PostMapping("/daily-pnl")
    public ResponseEntity<Map<String, Object>> triggerDailyPnl(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/daily-pnl", "일일 PnL 기록 완료", "일일 PnL 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/daily-pnl", result);
        return result;
    }

    @Operation(summary = "장중 변동성 돌파", description = "09:00~10:00 구간 돌파 종목 매수 (설정 시)")
    @PostMapping("/intraday-breakout")
    public ResponseEntity<Map<String, Object>> triggerIntradayBreakout(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/intraday-breakout", "장중 변동성 돌파 실행 완료", "장중 변동성 돌파 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/intraday-breakout", result);
        return result;
    }

    @Operation(summary = "중기 리밸런스", description = "MEDIUM_TERM 월 1회 리밸런싱 훅 (스텁)")
    @PostMapping("/medium-term-rebalance")
    public ResponseEntity<Map<String, Object>> triggerMediumTermRebalance(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/medium-term-rebalance", "중기 리밸런스 완료", "중기 리밸런스 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/medium-term-rebalance", result);
        return result;
    }

    @Operation(summary = "전략 거버넌스 검사", description = "최근 N개월 백테스트 실행 후 MDD/Sharpe 열화 시 Discord 알림 발송")
    @PostMapping("/strategy-governance-check")
    public ResponseEntity<Map<String, Object>> triggerStrategyGovernanceCheck(Principal principal) {
        ResponseEntity<Map<String, Object>> result = runTrigger("/strategy-governance-check", "전략 거버넌스 검사 완료", "전략 거버넌스 검사 실패", new JobParametersBuilder());
        recordManualTrigger(principal, "/strategy-governance-check", result);
        return result;
    }
}
