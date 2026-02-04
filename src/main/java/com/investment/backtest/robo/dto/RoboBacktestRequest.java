package com.investment.backtest.robo.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 로보 어드바이저 백테스트 실행 요청.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoboBacktestRequest {

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax(value = "1000000000000")
    private BigDecimal initialCapital;

    /** 자산 유니버스 (미지정 시 설정 기본값) */
    private List<String> assetSymbols;

    /** 모멘텀 기간 (개월). 미지정 시 12 */
    private Integer momentumMonths;

    /** 이동평균 창 (일). 미지정 시 200 */
    private Integer maWindowDays;

    /** 모멘텀 상위 N개. 미지정 시 4 */
    private Integer topN;

    /** MONTHLY, QUARTERLY. 미지정 시 MONTHLY */
    private String rebalanceFrequency;

    /** Drift 임계값 (%). 미지정 시 5 */
    private BigDecimal driftThresholdPct;

    /** 수수료 (%). 미지정 시 0.1 */
    private BigDecimal commPct;

    /** 슬리피지 (%). 미지정 시 0.2 */
    private BigDecimal slipPct;

    /** 무위험 수익률 (%). 미지정 시 0 */
    private BigDecimal riskFreeRatePct;

    /** 벤치마크 비중 (symbol → weight). 미지정 시 SPY=0.6, TLT=0.4 */
    private Map<String, BigDecimal> benchmarkWeights;

    /** 변동성 lookback (일). 미지정 시 60 */
    private Integer volatilityLookbackDays;

    /**
     * 듀얼 모멘텀 모드: MULTI_ASSET(기존), DUAL_MOMENTUM_NOTE(절대 12M vs T-bill + 상대 6M 섹터 상위
     * 2개). 미지정 시 설정 기본값
     */
    private String dualMomentumMode;

    /**
     * 리밸런싱 실행가: CLOSE(당일 종가), NEXT_OPEN(다음 거래일 시가). 미지정 시 CLOSE.
     */
    private String rebalanceExecutionPrice;
}
