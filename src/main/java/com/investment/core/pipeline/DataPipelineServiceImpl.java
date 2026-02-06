package com.investment.core.pipeline;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Phase 1: 데이터 파이프라인 스텁 — 기존 배치/스케줄러는 그대로 유지.
 * 진입점만 제공하고, 실제 수집은 기존 KrxCollectionService, UsMarketCollectionService 등이 수행.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPipelineServiceImpl implements DataPipelineService {

    @Override
    public void runIngestion(LocalDate asOfDate, String market) {
        log.debug("DataPipeline runIngestion: asOfDate={}, market={} (delegated to existing batch/schedulers)", asOfDate, market);
        // 기존 배치 태스크릿(UsDailyCollectTasklet, KrxDailyCollectTasklet 등)이 별도 스케줄로 동작.
        // 향후 단일 진입점으로 통합 시 여기서 해당 서비스 호출.
    }
}
