package com.investment.factor.scheduler;

import com.investment.factor.service.FactorCalculationService;
import com.investment.factor.service.UniverseFilterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 4단계 파이프라인 1·2단계 스케줄러.
 * 매일 장 시작 전: 1) 유니버스 필터 실행, 2) 전일 기준 팩터(시그널) 계산.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FactorCalculationScheduler {

    private static final String MARKET_KR = "KR";

    private final UniverseFilterService universeFilterService;
    private final FactorCalculationService factorCalculationService;

    /**
     * 매일 장 시작 전 (기본 08:00 KST) 실행.
     * 1) 유니버스 필터(유동성 통과 종목) → 2) 이격도·변동성 돌파·유동성 시그널 저장.
     */
    @Scheduled(cron = "${investment.factor.schedule-cron:0 0 8 * * *}")
    public void runFactorCalculation() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            universeFilterService.run(yesterday, MARKET_KR);
            int saved = factorCalculationService.calculateAndSave(yesterday);
            log.debug("팩터 계산 스케줄 완료: basDt={}, saved={}", yesterday, saved);
        } catch (Exception e) {
            log.warn("팩터 계산 스케줄 실패: {}", e.getMessage());
        }
    }
}
