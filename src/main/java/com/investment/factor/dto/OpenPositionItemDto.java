package com.investment.factor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 보유 포지션 목록용 DTO (자동투자 현황 표시).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenPositionItemDto {

    private Long positionId;
    private String symbol;
    private String market;
    private int quantity;
    private BigDecimal entryPrice;
    private LocalDate entryDt;

    /** 거래 사유: 진입 시그널 유형. 예: VOLATILITY_BREAKOUT, DUAL_MOMENTUM */
    private String signalType;

    /** 거래 사유: 청산 규칙 유형 (청산 시 설정). 예: ATR_TRAILING_STOP, TIME_CUT, STOP_LOSS */
    private String exitRuleType;

    /** 현재가 (자동투자 현황 표시용). 미조회 시 null. */
    private BigDecimal currentPrice;

    /** 손익률 % (자동투자 현황 표시용). 미계산 시 null. */
    private BigDecimal pnlPercent;
}
