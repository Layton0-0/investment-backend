package com.investment.backtest.robo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * US 일봉 수집 요청. 미입력 시 최근 30일로 수집.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectUsDailyRequest {

    /** 수집 시작일 (포함). null이면 endDate 기준 30일 전 */
    private LocalDate startDate;
    /** 수집 종료일 (포함). null이면 오늘 */
    private LocalDate endDate;
}
