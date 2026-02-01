package com.investment.tradingportfolio.scheduler;

import com.investment.tradingportfolio.service.TradingPortfolioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * 트레이딩 포트폴리오 자동 생성 스케줄러
 * 매일 09:00 KST 실행 (팩터 계산 08:00 이후, 당일 시그널 기반)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingPortfolioScheduler {
    
    private final TradingPortfolioService tradingPortfolioService;
    
    /**
     * 매일 한국 시간 오전 9시에 오늘의 트레이딩 포트폴리오 생성
     * 팩터 계산(08:00) 이후 실행하여 당일 TB_SIGNAL_SCORE 기반으로 생성
     * cron 표현식: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void generateDailyPortfolio() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
        
        log.info("일별 트레이딩 포트폴리오 자동 생성 시작: date={}, time={}", today, now);
        
        try {
            tradingPortfolioService.generateDailyPortfolio(today);
            log.info("일별 트레이딩 포트폴리오 자동 생성 완료: date={}", today);
        } catch (Exception e) {
            log.error("일별 트레이딩 포트폴리오 자동 생성 실패: date={}", today, e);
        }
    }
}
