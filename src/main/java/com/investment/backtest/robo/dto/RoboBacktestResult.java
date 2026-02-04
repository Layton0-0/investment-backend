package com.investment.backtest.robo.dto;

import com.investment.backtest.dto.DateEquityPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 로보 어드바이저 백테스트 실행 결과 — 메트릭·수익 곡선·벤치마크·리밸런싱 이력.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoboBacktestResult {

    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialCapital;
    private BigDecimal finalEquity;
    private BigDecimal totalReturnPct;
    private BigDecimal cagr;
    private BigDecimal mddPct;
    private BigDecimal sharpeRatio;
    private BigDecimal calmarRatio;
    /** 연간화 회전율 (%) */
    private BigDecimal turnover;
    private BigDecimal benchmarkCagr;
    private BigDecimal benchmarkMddPct;
    /** 전략 수익 곡선 (일자별 자산) */
    private List<DateEquityPoint> equityCurve;
    /** 벤치마크 수익 곡선 (일자별 가치) */
    private List<DateEquityPoint> benchmarkCurve;
    /** 리밸런싱 이력 */
    private List<RebalanceSnapshotDto> rebalanceHistory;
    /** 데이터 부재·평평한 곡선 등 안내 메시지 (선택) */
    private String warningMessage;
}
