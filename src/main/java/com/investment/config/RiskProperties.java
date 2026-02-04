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

    /** VIX 임계값. 초과 시 고변동성으로 간주 (기본 30) */
    private BigDecimal vixThreshold = new BigDecimal("30");

    /** 고변동성 시 신규 매수 비중 축소 비율 (%, 기본 50 = 50%만 투입) */
    private BigDecimal reduceSizeOnHighVolPct = new BigDecimal("50");

    /** 일일 손실 한도 (%, 당일 시작 자산 대비). 초과 시 당일 신규 매수 중단 */
    private BigDecimal dailyLossLimitPct = new BigDecimal("5");

    /** 거시경제 지표 API URL (선택). GET JSON 예: {"vix": 18.5}. 미설정 시 VIX 미제공(기존 동작). */
    private String macroIndicatorUrl;
}
