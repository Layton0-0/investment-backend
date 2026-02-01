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
 * KR/US 시장 모두 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FactorCalculationScheduler {

    private static final String MARKET_KR = "KR";
    private static final String MARKET_US = "US";

    private final UniverseFilterService universeFilterService;
    private final FactorCalculationService factorCalculationService;

    /**
     * 매일 장 시작 전 (기본 08:00 KST) 실행.
     * KR/US 시장별로 1) 유니버스 필터(유동성 통과 종목) → 2) 이격도·변동성 돌파·유동성 시그널 저장.
     */
    @Scheduled(cron = "${investment.factor.schedule-cron:0 0 8 * * *}")
    public void runFactorCalculation() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        
        // KR 시장 처리
        processMarket(yesterday, MARKET_KR);
        
        // US 시장 처리
        processMarket(yesterday, MARKET_US);
    }

    /**
     * 시장별 유니버스 필터 및 팩터 계산 실행.
     * 한 시장 실패해도 다른 시장은 계속 실행.
     *
     * @param basDt 기준일
     * @param market 시장 (KR, US)
     */
    private void processMarket(LocalDate basDt, String market) {
        try {
            universeFilterService.run(basDt, market);
            int saved = factorCalculationService.calculateAndSave(basDt, market);
            log.info("팩터 계산 완료: market={}, basDt={}, saved={}", market, basDt, saved);
        } catch (Exception e) {
            log.warn("팩터 계산 실패: market={}, basDt={}, error={}", market, basDt, e.getMessage(), e);
        }
    }
}
