package com.investment.risk.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 성과 귀인(Performance Attribution) API 응답.
 * 팩터(signalType)·전략(strategyType)별 실현 손익 기여율(합 100%).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "팩터/전략별 수익 기여도. 청산 포지션 기준 실현 손익 집계.")
public class PerformanceAttributionDto {

    @Schema(description = "총 실현 손익 (청산 포지션 합계)")
    private BigDecimal totalRealizedPnl;

    @Schema(description = "시그널 타입(팩터)별 기여 비율. 키: signalType, 값: 기여율 0~100")
    private Map<String, BigDecimal> bySignalType;

    @Schema(description = "전략 타입별 기여 비율. 키: strategyType, 값: 기여율 0~100")
    private Map<String, BigDecimal> byStrategyType;
}
