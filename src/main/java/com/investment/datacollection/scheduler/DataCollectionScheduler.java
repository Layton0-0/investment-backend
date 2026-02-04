package com.investment.datacollection.scheduler;

import com.investment.datacollection.service.DartCollectionService;
import com.investment.datacollection.service.KrxCollectionService;
import com.investment.datacollection.service.SecCollectionService;
import com.investment.datacollection.service.UsMarketCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 데이터 수집 스케줄러
 * DART/KRX/SEC EDGAR/US 시장/Yahoo 원천별 주기 실행. 원천 장애 시 해당 원천만 스킵(Fallback).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectionScheduler {

    private final DartCollectionService dartCollectionService;
    private final KrxCollectionService krxCollectionService;
    private final SecCollectionService secCollectionService;
    private final UsMarketCollectionService usMarketCollectionService;

    /**
     * DART 공시 수집 (10분마다). Spring Batch Job에서 호출.
     */
    public void collectDart() {
        try {
            dartCollectionService.collectAndSave();
        } catch (Exception e) {
            log.warn("DART 수집 실패(다음 주기 재시도): {}", e.getMessage());
        }
    }

    /**
     * SEC EDGAR 공시 수집 (15분마다). Spring Batch Job에서 호출.
     */
    public void collectSec() {
        try {
            secCollectionService.collectAndSave();
        } catch (Exception e) {
            log.warn("SEC EDGAR 수집 실패(다음 주기 재시도): {}", e.getMessage());
        }
    }

    /**
     * KRX 일별 시세 수집 (매일 장 마감 후, 기본 16:00 KST). Spring Batch Job에서 호출.
     */
    public void collectKrxDaily() {
        try {
            LocalDate today = LocalDate.now();
            krxCollectionService.collectAndSave(today);
        } catch (Exception e) {
            log.warn("KRX 일별 수집 실패(다음 주기 재시도): {}", e.getMessage());
        }
    }

    /**
     * US 시장 일별 시세 수집 (매일 장 마감 후, 기본 17:00 KST). Spring Batch Job에서 호출.
     */
    public void collectUsDaily() {
        try {
            LocalDate today = LocalDate.now();
            usMarketCollectionService.collectAndSave(today);
        } catch (Exception e) {
            log.warn("US 시장 일별 수집 실패(다음 주기 재시도): {}", e.getMessage());
        }
    }
}
