package com.investment.core.pipeline;

import java.time.LocalDate;

/**
 * 데이터 수집·정제 파이프라인 진입점.
 * 시장 데이터(MarketDataClient), 수집(Krx, Us, Dart 등) 오케스트레이션.
 * 수정주가 정책은 한투 API 호출 시 FID_ORG_ADJ_PRC=0 사용으로 명시.
 */
public interface DataPipelineService {

    /**
     * 기준일 기준 시장 데이터 수집·정제 트리거.
     * 배치 또는 스케줄러에서 호출.
     *
     * @param asOfDate 기준일
     * @param market   시장 (KR, US) 또는 null(전체)
     */
    void runIngestion(LocalDate asOfDate, String market);
}
