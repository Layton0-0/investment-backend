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
                // 다음 월요일 찾기
                while (next.getDayOfWeek().getValue() != 1 || next.isBefore(now) || next.equals(now)) {
                    next = next.plusDays(1);
                }
                return next.toLocalDateTime();
            }
        } catch (Exception e) {
            log.warn("다음 실행 시간 계산 실패: cron={}", cronExpression, e);
        }
        
        return now.plusHours(1).toLocalDateTime();
    }
}
