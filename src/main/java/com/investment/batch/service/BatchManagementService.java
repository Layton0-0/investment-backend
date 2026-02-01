package com.investment.batch.service;

import com.investment.batch.dto.BatchJobDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 배치 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchManagementService {
    
    /**
     * 모든 배치 작업 목록 조회
     */
    public List<BatchJobDto> getAllBatchJobs() {
        List<BatchJobDto> jobs = new ArrayList<>();
        
        // 1. 트레이딩 포트폴리오 생성 스케줄러
        jobs.add(BatchJobDto.builder()
                .id("trading-portfolio-generator")
                .name("트레이딩 포트폴리오 생성")
                .description("매일 한국 시간 오전 6시에 오늘의 트레이딩 포트폴리오를 자동 생성합니다.")
                .cronExpression("0 0 6 * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());
        
        // 2. 단기 전략 실행 스케줄러
        jobs.add(BatchJobDto.builder()
                .id("short-term-strategy-executor")
                .name("단기 전략 실행")
                .description("매 1시간마다 활성화된 단기 전략을 실행합니다.")
                .cronExpression("0 0 * * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());
        
        // 3. 중기 전략 실행 스케줄러
        jobs.add(BatchJobDto.builder()
                .id("medium-term-strategy-executor")
                .name("중기 전략 실행")
                .description("매일 오전 9시에 활성화된 중기 전략을 실행합니다.")
                .cronExpression("0 0 9 * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());
        
        // 4. 장기 전략 실행 스케줄러
        jobs.add(BatchJobDto.builder()
                .id("long-term-strategy-executor")
                .name("장기 전략 실행")
                .description("매주 월요일 오전 9시에 활성화된 장기 전략을 실행합니다.")
                .cronExpression("0 0 9 * * MON")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 5. DART 공시 수집
        jobs.add(BatchJobDto.builder()
                .id("dart-disclosure-collector")
                .name("DART 공시 수집")
                .description("10분마다 Open DART 공시 목록을 수집하여 TB_NEWS_ITEMS에 저장합니다.")
                .cronExpression("0 */10 * * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 6. SEC EDGAR 공시 수집
        jobs.add(BatchJobDto.builder()
                .id("sec-disclosure-collector")
                .name("SEC EDGAR 공시 수집")
                .description("15분마다 SEC EDGAR 공시 목록을 수집합니다.")
                .cronExpression("0 */15 * * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 7. KRX 일별 시세 수집
        jobs.add(BatchJobDto.builder()
                .id("krx-daily-collector")
                .name("KRX 일별 시세 수집")
                .description("매일 장 마감 후(16:00 KST) KRX 일별 시세를 수집합니다.")
                .cronExpression("0 0 16 * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 8. US 시장 일별 시세 수집
        jobs.add(BatchJobDto.builder()
                .id("us-daily-collector")
                .name("US 시장 일별 시세 수집")
                .description("매일 미국 장 마감 후(17:00 KST) US 일별 시세를 수집합니다.")
                .cronExpression("0 0 17 * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 9. 팩터 계산 (유니버스·시그널)
        jobs.add(BatchJobDto.builder()
                .id("factor-calculation")
                .name("팩터 계산")
                .description("매일 장 시작 전(08:00 KST) 유니버스 필터 및 팩터(시그널) 계산을 실행합니다.")
                .cronExpression("0 0 8 * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 10. 파이프라인 실행
        jobs.add(BatchJobDto.builder()
                .id("pipeline-execution")
                .name("파이프라인 실행")
                .description("장 시작 후(09:10 KST) 4단계 파이프라인(단/중/장기 배분)을 실행합니다.")
                .cronExpression("0 10 9 * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 11. 파이프라인 청산 평가
        jobs.add(BatchJobDto.builder()
                .id("pipeline-exit")
                .name("파이프라인 청산 평가")
                .description("장중 평일 5분마다 보유 포지션 청산 규칙을 평가하고 매도 시그널 시 주문 실행합니다.")
                .cronExpression("0 */5 9-15 * * MON-FRI")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 12. 체결 확인 후 포지션 등록
        jobs.add(BatchJobDto.builder()
                .id("fill-confirmation")
                .name("체결 확인 후 포지션 등록")
                .description("매분 체결된 주문에 대해 포지션 등록을 수행합니다.")
                .cronExpression("0 * * * * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 13. 중기 리밸런스
        jobs.add(BatchJobDto.builder()
                .id("medium-term-rebalance")
                .name("중기 리밸런스")
                .description("매월 1일 08:30 KST에 중기 전략 리밸런스를 실행합니다(스텁).")
                .cronExpression("0 30 8 1 * *")
                .timeZone("Asia/Seoul")
                .status("ACTIVE")
                .executionCount(0L)
                .successCount(0L)
                .failureCount(0L)
                .build());

        // 다음 실행 시간 계산
        jobs.forEach(job -> {
            try {
                CronTrigger trigger = new CronTrigger(job.getCronExpression(), 
                        ZoneId.of(job.getTimeZone()));
                ZonedDateTime now = ZonedDateTime.now(ZoneId.of(job.getTimeZone()));
                // 간단한 다음 실행 시간 계산 (실제로는 TaskScheduler를 사용해야 함)
                job.setNextExecutionTime(calculateNextExecution(job.getCronExpression(), now));
            } catch (Exception e) {
                log.warn("다음 실행 시간 계산 실패: jobId={}", job.getId(), e);
            }
        });
        
        return jobs;
    }
    
    /**
     * 다음 실행 시간 계산 (간단한 버전)
     */
    private LocalDateTime calculateNextExecution(String cronExpression, ZonedDateTime now) {
        // 실제로는 CronExpression을 파싱하여 정확히 계산해야 하지만,
        // 여기서는 간단한 예시만 제공
        try {
            // cron: "0 0 6 * * *" -> 매일 6시
            if (cronExpression.equals("0 0 6 * * *")) {
                ZonedDateTime next = now.withHour(6).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 0 * * * *" -> 매 시간
            else if (cronExpression.equals("0 0 * * * *")) {
                return now.plusHours(1).withMinute(0).withSecond(0).toLocalDateTime();
            }
            // cron: "0 0 9 * * *" -> 매일 9시
            else if (cronExpression.equals("0 0 9 * * *")) {
                ZonedDateTime next = now.withHour(9).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 */10 * * * *" -> 10분마다
            else if (cronExpression.startsWith("0 */10")) {
                int minute = now.getMinute();
                int nextMin = ((minute / 10) + 1) * 10;
                if (nextMin >= 60) {
                    return now.plusHours(1).withMinute(0).withSecond(0).toLocalDateTime();
                }
                return now.withMinute(nextMin).withSecond(0).toLocalDateTime();
            }
            // cron: "0 0 9 * * MON" -> 매주 월요일 9시
            else if (cronExpression.equals("0 0 9 * * MON")) {
                ZonedDateTime next = now.withHour(9).withMinute(0).withSecond(0);
                while (next.getDayOfWeek().getValue() != 1 || next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 */15 * * * *" -> 15분마다
            else if (cronExpression.startsWith("0 */15")) {
                int minute = now.getMinute();
                int nextMin = ((minute / 15) + 1) * 15;
                if (nextMin >= 60) {
                    return now.plusHours(1).withMinute(0).withSecond(0).toLocalDateTime();
                }
                return now.withMinute(nextMin).withSecond(0).toLocalDateTime();
            }
            // cron: "0 0 16 * * *" -> 매일 16시
            else if (cronExpression.equals("0 0 16 * * *")) {
                ZonedDateTime next = now.withHour(16).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 0 17 * * *" -> 매일 17시
            else if (cronExpression.equals("0 0 17 * * *")) {
                ZonedDateTime next = now.withHour(17).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 0 8 * * *" -> 매일 8시
            else if (cronExpression.equals("0 0 8 * * *")) {
                ZonedDateTime next = now.withHour(8).withMinute(0).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 10 9 * * *" -> 매일 09:10
            else if (cronExpression.equals("0 10 9 * * *")) {
                ZonedDateTime next = now.withHour(9).withMinute(10).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 */5 9-15 * * MON-FRI" -> 평일 장중 5분마다 (다음 분 5의 배수)
            else if (cronExpression.contains("*/5") && cronExpression.contains("9-15")) {
                int minute = now.getMinute();
                int nextMin = ((minute / 5) + 1) * 5;
                ZonedDateTime next = nextMin >= 60 ? now.plusHours(1).withMinute(0) : now.withMinute(nextMin);
                next = next.withSecond(0);
                if (next.getHour() < 9) next = next.withHour(9).withMinute(0);
                if (next.getHour() > 15) next = next.plusDays(1).withHour(9).withMinute(0);
                if (next.getDayOfWeek().getValue() >= 6) {
                    next = next.plusDays(8 - next.getDayOfWeek().getValue()).withHour(9).withMinute(0);
                }
                return next.toLocalDateTime();
            }
            // cron: "0 * * * * *" -> 매분
            else if (cronExpression.equals("0 * * * * *")) {
                return now.plusMinutes(1).withSecond(0).toLocalDateTime();
            }
            // cron: "0 30 8 1 * *" -> 매월 1일 08:30
            else if (cronExpression.equals("0 30 8 1 * *")) {
                ZonedDateTime next = now.withDayOfMonth(1).withHour(8).withMinute(30).withSecond(0);
                if (next.isBefore(now) || next.equals(now)) {
                    next = now.plusMonths(1).withDayOfMonth(1).withHour(8).withMinute(30).withSecond(0);
                }
                return next.toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("다음 실행 시간 계산 실패: cron={}", cronExpression, e);
        }

        return now.plusHours(1).toLocalDateTime();
    }
}
