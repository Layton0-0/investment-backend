package com.investment.risk.service;

import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.risk.dto.MacroIndicatorDto;

import java.time.LocalDate;
import java.util.List;

/**
 * 매크로 지표 대시보드 서비스.
 * 거시경제 지표를 종합 조회하고 시장 상태를 모니터링합니다.
 */
public interface MacroDashboardService {

    /**
     * 매크로 대시보드 전체 조회.
     *
     * @return 대시보드 응답 (모든 지표 + 시장 상태)
     */
    MacroDashboardResponse getDashboard();

    /**
     * 개별 지표 조회.
     *
     * @param indicatorCode 지표 코드 (예: VIX, US10Y)
     * @return 지표 정보
     */
    MacroIndicatorDto getIndicator(String indicatorCode);

    /**
     * 지표 히스토리 조회.
     *
     * @param indicatorCode 지표 코드
     * @param startDate     시작일
     * @param endDate       종료일
     * @return 기간별 지표 목록
     */
    List<MacroIndicatorDto> getIndicatorHistory(String indicatorCode, LocalDate startDate, LocalDate endDate);

    /**
     * 현재 시장 상태(레짐) 조회.
     *
     * @return 시장 상태 (BULL/BEAR/NEUTRAL)
     */
    MacroDashboardResponse.MarketRegime getMarketRegime();

    /**
     * 캐시 갱신.
     */
    void refreshCache();

    /**
     * 지원되는 모든 지표 코드 목록.
     *
     * @return 지표 코드 목록
     */
    List<String> getSupportedIndicatorCodes();
}
