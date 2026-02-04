package com.investment.backtest.robo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 로보 어드바이저 목표 비중 산출 결과 (공통 엔진 출력).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoboAllocationResult {

    /** 기준일 */
    private LocalDate asOfDate;
    /** 자산별 목표 비중 (symbol → weight, 합 1 미만이면 나머지는 현금) */
    private Map<String, BigDecimal> weights;
    /** 현금 비중 (0~1) */
    private BigDecimal cashWeight;
}
