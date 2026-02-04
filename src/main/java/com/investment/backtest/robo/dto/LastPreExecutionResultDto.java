package com.investment.backtest.robo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 실행 전 백테스트 최근 결과 (UI 노출용).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LastPreExecutionResultDto {

    private String accountNo;
    private boolean passed;
    private BigDecimal mddPct;
    private BigDecimal sharpeRatio;
    private Instant runAt;
}
