package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 로보어드바이저 동적 자산배분 백테스트·실행 전 검증 설정.
 */
@Component
@ConfigurationProperties(prefix = "investment.backtest.robo")
@Getter
@Setter
public class RoboBacktestProperties {

    /** 자산 유니버스 (쉼표 구분). 기본 SPY,IEF,TLT,GLD,DBC,BIL */
    private String assetSymbols = "SPY,IEF,TLT,GLD,DBC,BIL";

    /** 파싱된 자산 심볼 목록 (공백 제거, 빈 문자열 제외) */
    public List<String> getAssetSymbolList() {
        if (assetSymbols == null || assetSymbols.isBlank()) {
            return List.of("SPY", "IEF", "TLT", "GLD", "DBC", "BIL");
        }
        return Arrays.stream(assetSymbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** 모멘텀 기간 (개월). 기본 12 */
    private int momentumMonths = 12;
    /** 이동평균 창 (일). 기본 200 */
    private int maWindowDays = 200;
    /** 모멘텀 상위 N개 자산만 투자. 기본 4 */
    private int topN = 4;
    /** 리밸런싱 주기: MONTHLY, QUARTERLY */
    private String rebalanceFrequency = "MONTHLY";
    /** 목표 비중 대비 괴리 시 수시 리밸런싱 임계값 (%). 기본 5 */
    private BigDecimal driftThresholdPct = new BigDecimal("5");
    /** 거래 수수료 (%). 기본 0.1 */
    private BigDecimal commPct = new BigDecimal("0.1");
    /** 슬리피지 (%). 기본 0.2 */
    private BigDecimal slipPct = new BigDecimal("0.2");
    /** 무위험 수익률 (%). 샤프 계산용. 기본 0 */
    private BigDecimal riskFreeRatePct = BigDecimal.ZERO;
    /** 벤치마크 비중 (예: SPY=0.6,TLT=0.4). 문자열로 저장, 파싱은 서비스에서 */
    private String benchmarkWeights = "SPY=0.6,TLT=0.4";
    /** 변동성 계산 lookback (일). 기본 60 */
    private int volatilityLookbackDays = 60;
    /** 실행 전 백테스트 lookback (개월). 기본 12 */
    private int preExecutionLookbackMonths = 12;
    /** 실행 전 백테스트 MDD 임계값 (절대값, 예: 15 → -15% 이하여야 통과). 기본 15 */
    private BigDecimal preExecutionMaxMddPct = new BigDecimal("15");
    /** 실행 전 백테스트 최소 샤프. 기본 0.5 */
    private BigDecimal preExecutionMinSharpe = new BigDecimal("0.5");
    /** 로보 리밸런싱 스케줄 cron. 기본 매월 말일 09:00 KST */
    private String rebalanceScheduleCron = "0 0 9 L * *";

    /**
     * 듀얼 모멘텀 모드: MULTI_ASSET(기존), DUAL_MOMENTUM_NOTE(절대 12M vs T-bill + 상대 6M 섹터 상위
     * 2개)
     */
    private String dualMomentumMode = "MULTI_ASSET";
    /** 듀얼 모멘텀(노트) 모드 시 섹터 ETF 유니버스 (쉼표 구분). 기본 XLK,XLE,XLF,XLV,XLI,XLY,XLP,XLB */
    private String sectorEtfSymbols = "XLK,XLE,XLF,XLV,XLI,XLY,XLP,XLB";
    /** 상대 모멘텀 기간 (개월). 노트 모드 시 6 */
    private int momentumMonthsRelative = 6;
    /** 상대 모멘텀 상위 N개. 노트 모드 시 2 */
    private int topNSector = 2;
    /** 절대 모멘텀 시장 지표 (S&P500 대리). 기본 SPY */
    private String absoluteMomentumSymbol = "SPY";
    /** 무위험 자산 심볼 (T-bill 대리). 기본 BIL */
    private String riskFreeSymbol = "BIL";
    /** 리밸런싱 실행가: CLOSE(당일 종가), NEXT_OPEN(다음 거래일 시가). 기본 CLOSE */
    private String rebalanceExecutionPrice = "CLOSE";
    /** 실제 ETF 주문 실행 여부. false면 목표 비중 로깅만 (기본 false) */
    private boolean executeOrders = false;
    /** 최소 주문 금액(USD). 이 금액 미만 차이는 주문 스킵. 기본 50 */
    private java.math.BigDecimal minOrderAmountUsd = new java.math.BigDecimal("50");

    public List<String> getSectorEtfSymbolList() {
        if (sectorEtfSymbols == null || sectorEtfSymbols.isBlank()) {
            return List.of("XLK", "XLE", "XLF", "XLV", "XLI", "XLY", "XLP", "XLB");
        }
        return Arrays.stream(sectorEtfSymbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
