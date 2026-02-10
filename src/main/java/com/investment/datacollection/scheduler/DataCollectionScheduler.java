package com.investment.datacollection.scheduler;

import com.investment.datacollection.service.KrxCollectionService;
import com.investment.datacollection.service.UsMarketCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 데이터 수집 스케줄러 (KRX, US 시장만. DART/SEC는 Python 수집기에서 수행)
 * 원천 장애 시 해당 원천만 스킵(Fallback).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectionScheduler {

    private final KrxCollectionService krxCollectionService;
    private final UsMarketCollectionService usMarketCollectionService;

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
