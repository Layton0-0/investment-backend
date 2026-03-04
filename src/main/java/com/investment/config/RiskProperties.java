package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 리스크 게이트·일일 손실 한도 설정 (전문 투자자 흐름 P0).
 */
@Component
@ConfigurationProperties(prefix = "investment.risk")
@Getter
@Setter
public class RiskProperties {

    /** 레짐 게이트 사용 여부. true면 VIX 등 초과 시 신규 매수 비중 축소 또는 스킵 */
    private boolean regimeGateEnabled = false;

    /** VIX/이평선 기반 시장 레짐 탐지 사용 여부. true면 RegimeDetectionService로 BULL/BEAR/NEUTRAL 판정 */
    private boolean regimeDetectionEnabled = true;

    /** VIX 임계값. 초과 시 고변동성으로 간주 (기본 30) */
    private BigDecimal vixThreshold = new BigDecimal("30");

    /** 고변동성 시 신규 매수 비중 축소 비율 (%, 기본 50 = 50%만 투입) */
    private BigDecimal reduceSizeOnHighVolPct = new BigDecimal("50");

    /** 일일 손실 한도 (%, 당일 시작 자산 대비). 초과 시 당일 신규 매수 중단 */
    private BigDecimal dailyLossLimitPct = new BigDecimal("5");

    /** 시장 급락 게이트 사용 여부. true면 벤치마크 지수 일일 낙폭이 임계값 이상일 때 당일 신규 매수 중단 */
    private boolean marketCrashGateEnabled = true;

    /** 시장 급락 임계값 (%, 전일 대비 벤치마크 일일 수익률). 이 값 이상 하락 시 신규 매수 중단 (기본 5 = -5%) */
    private BigDecimal marketCrashDailyDropPct = new BigDecimal("5");

    /** 시장 급락 판단용 벤치마크 심볼 (기본 SPY) */
    private String marketCrashBenchmarkSymbol = "SPY";

    /** 시장 급락 게이트 벤치마크 시장 (기본 US) */
    private String marketCrashBenchmarkMarket = "US";

    /** 거시경제 지표 API URL (선택). GET JSON 예: {"vix": 18.5}. 미설정 시 VIX 미제공(기존 동작). */
    private String macroIndicatorUrl;

    /** VaR/CVaR 단순 파라메트릭용 일일 변동성 가정 (%). 기본 1.0 = 1%. 0 또는 미설정 시 VaR/CVaR 미산출 */
    private BigDecimal varDailyVolPct;

    /** VaR 방법론 (PARAMETRIC | HISTORICAL). 기본 PARAMETRIC */
    private VarMethod varMethod = VarMethod.PARAMETRIC;

    /** 역사적 VaR 룩백 기간 (거래일). 기본 252 (1년). HISTORICAL 방법 선택 시 사용 */
    private int varLookbackDays = 252;

    /** 연간 손실 한도 비율 (%, 연초 자산 대비). 기본 20%. 초과 시 신규 매수 중단 */
    private BigDecimal yearEndLossLimitPct = new BigDecimal("20");

    /** 연간 손실 한도 알림 임계값 (0~1). 기본 0.8 = 한도의 80% 도달 시 알림 */
    private BigDecimal yearEndAlertThresholdPct = new BigDecimal("0.8");

    /** 리스크 이벤트 알림: 일일 손실 한도 대비 이 비율(0~1) 도달 시 알림. 기본 0.8 = 80% 도달 시 */
    private BigDecimal alertMddThresholdPct = new BigDecimal("0.8");

    /** VaR 계산 방법론 */
    public enum VarMethod {
        /** 파라메트릭 VaR (정규분포 가정) */
        PARAMETRIC,
        /** 역사적 VaR (실제 수익률 분포 사용) */
        HISTORICAL
    }

    /** 리스크 이벤트 알림: VaR 95% 초과 시 Discord 알림 사용 여부. 기본 true */
    private boolean alertVarExceedEnabled = true;

    /** 드로다운 회복 모드(P6-1): MDD가 이 값(0~1) 이상이면 회복 모드 ON. 기본 0.10 = 10% */
    private BigDecimal drawdownRecoveryThresholdPct = new BigDecimal("0.10");

    /** 드로다운 회복 모드 해제: MDD가 이 값 이하로 회복되면 정상 모드. 기본 0.05 = 5% */
    private BigDecimal drawdownRecoveryExitPct = new BigDecimal("0.05");

    /** 드로다운 회복 모드 시 신규 매수 권장 금액 스케일 (0~1). 기본 0.5 = 50% */
    private BigDecimal drawdownRecoveryScale = new BigDecimal("0.5");
}
