package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 스트레스 테스트 시나리오 정의.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StressScenario {

    private String code;
    private String name;
    private String description;

    /**
     * 자산군별 충격률 (예: {"EQUITY": -0.40, "BOND": -0.05, "COMMODITY": -0.30}).
     * 음수: 하락, 양수: 상승.
     */
    private Map<String, BigDecimal> assetClassShocks;

    /**
     * 개별 종목별 충격률 (선택, 예: {"AAPL": -0.45}).
     */
    private Map<String, BigDecimal> symbolShocks;

    /**
     * 위기 시 상관계수 상승률 (예: 0.3 → 기존 상관관계 + 30% 증가).
     */
    private BigDecimal correlationIncrease;

    /**
     * VIX 충격 수준 (예: 80).
     */
    private BigDecimal vixLevel;

    /**
     * 시나리오 기간 (일수).
     */
    private int durationDays;

    /**
     * 기본 시나리오 - 2008 금융위기.
     */
    public static StressScenario financialCrisis2008() {
        return StressScenario.builder()
                .code("FINANCIAL_CRISIS_2008")
                .name("2008 금융위기")
                .description("서브프라임 모기지 사태로 인한 글로벌 금융위기. S&P 500 -56.8%, VIX 80+")
                .assetClassShocks(Map.of(
                        "EQUITY", new BigDecimal("-0.50"),
                        "BOND", new BigDecimal("0.05"),
                        "COMMODITY", new BigDecimal("-0.40"),
                        "REAL_ESTATE", new BigDecimal("-0.35"),
                        "CASH", BigDecimal.ZERO
                ))
                .correlationIncrease(new BigDecimal("0.40"))
                .vixLevel(new BigDecimal("80"))
                .durationDays(365)
                .build();
    }

    /**
     * 기본 시나리오 - 2020 코로나 폭락.
     */
    public static StressScenario covidCrash2020() {
        return StressScenario.builder()
                .code("COVID_CRASH_2020")
                .name("2020 코로나 폭락")
                .description("COVID-19 팬데믹으로 인한 급락. S&P 500 -34% (1개월 내), VIX 82")
                .assetClassShocks(Map.of(
                        "EQUITY", new BigDecimal("-0.35"),
                        "BOND", new BigDecimal("0.08"),
                        "COMMODITY", new BigDecimal("-0.25"),
                        "REAL_ESTATE", new BigDecimal("-0.15"),
                        "CASH", BigDecimal.ZERO
                ))
                .correlationIncrease(new BigDecimal("0.35"))
                .vixLevel(new BigDecimal("82"))
                .durationDays(30)
                .build();
    }

    /**
     * 기본 시나리오 - 2022 금리 인상기.
     */
    public static StressScenario rateHike2022() {
        return StressScenario.builder()
                .code("RATE_HIKE_2022")
                .name("2022 금리 인상기")
                .description("연준 공격적 금리 인상(0%→5.5%). S&P 500 -25%, NASDAQ -33%, 채권 -15%")
                .assetClassShocks(Map.of(
                        "EQUITY", new BigDecimal("-0.25"),
                        "GROWTH_EQUITY", new BigDecimal("-0.35"),
                        "BOND", new BigDecimal("-0.15"),
                        "COMMODITY", new BigDecimal("0.10"),
                        "REAL_ESTATE", new BigDecimal("-0.20"),
                        "CASH", BigDecimal.ZERO
                ))
                .correlationIncrease(new BigDecimal("0.25"))
                .vixLevel(new BigDecimal("35"))
                .durationDays(270)
                .build();
    }

    /**
     * 기본 시나리오 - 블랙 먼데이 (1987).
     */
    public static StressScenario blackMonday1987() {
        return StressScenario.builder()
                .code("BLACK_MONDAY_1987")
                .name("1987 블랙 먼데이")
                .description("1987년 10월 19일 하루 만에 다우존스 -22.6%")
                .assetClassShocks(Map.of(
                        "EQUITY", new BigDecimal("-0.22"),
                        "BOND", new BigDecimal("0.03"),
                        "COMMODITY", new BigDecimal("-0.10"),
                        "CASH", BigDecimal.ZERO
                ))
                .correlationIncrease(new BigDecimal("0.50"))
                .vixLevel(new BigDecimal("150"))
                .durationDays(1)
                .build();
    }
}
