package com.investment.setting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 거래 설정 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradingSettingDto {

    @NotNull(message = "최대 투자금액은 필수입니다")
    @Positive(message = "최대 투자금액은 양수여야 합니다")
    private BigDecimal maxInvestmentAmount;

    /** 최소 투자금액. null이면 시스템 기본값 사용(퀀트 프로그램이 결정). */
    @Positive(message = "최소 투자금액은 양수여야 합니다")
    private BigDecimal minInvestmentAmount;

    @NotNull(message = "기본 통화는 필수입니다")
    private String defaultCurrency;

    private Boolean autoTradingEnabled;

    /** 로보 어드바이저 사용 여부. true면 로보 리밸런싱 스케줄러 대상 */
    private Boolean roboAdvisorEnabled;

    private BigDecimal riskLevel; // 0.0 ~ 1.0

    /**
     * 단기 비율 (0~1). 세 비율 합=1. NULL이면 스케줄러 기본값 0.2/0.4/0.4 사용.
     */
    private BigDecimal shortTermRatio;

    /**
     * 중기 비율 (0~1).
     */
    private BigDecimal mediumTermRatio;

    /**
     * 장기 비율 (0~1).
     */
    private BigDecimal longTermRatio;

    /** 파이프라인 자동 실행 허용. null이면 서버 기본값 사용 */
    private Boolean pipelineAutoExecute;

    /** 실계좌 자동 실행 허용. null이면 서버 기본값 사용 */
    private Boolean pipelineAllowRealExecution;
}
