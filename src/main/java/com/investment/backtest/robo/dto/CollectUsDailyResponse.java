package com.investment.backtest.robo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * US 일봉 수집 결과.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectUsDailyResponse {

    /** 수집한 일수 */
    private int collectedDays;
    /** 총 저장 건수 (일별 × 종목) */
    private int savedTotal;
    /** 안내 메시지 (스크립트 미설정 등) */
    private String message;
}
