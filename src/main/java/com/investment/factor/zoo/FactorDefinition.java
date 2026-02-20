package com.investment.factor.zoo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Factor Zoo 팩터 정의.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FactorDefinition {

    private String code;
    private String name;
    private String description;
    private FactorCategory category;
    private FactorDirection direction;
    private BigDecimal defaultWeight;
    private List<String> requiredFields;
    private Map<String, Object> parameters;

    public enum FactorCategory {
        VALUE,        // PBR, PER, EV/EBITDA
        MOMENTUM,     // 3M/6M/12M 수익률
        QUALITY,      // ROE, 영업이익률, 부채비율
        SIZE,         // 시가총액
        VOLATILITY,   // 변동성, Beta
        LIQUIDITY,    // 거래대금, 회전율
        TECHNICAL,    // 이격도, 변동성 돌파
        FLOW          // 수급 강도
    }

    public enum FactorDirection {
        HIGHER_BETTER,   // 높을수록 좋음 (ROE, 모멘텀 등)
        LOWER_BETTER,    // 낮을수록 좋음 (PER, 변동성 등)
        NEUTRAL          // 방향성 없음 (정보 제공용)
    }

    public static FactorDefinition pbr() {
        return FactorDefinition.builder()
                .code("PBR")
                .name("주가순자산비율")
                .description("시가총액 / 순자산. 낮을수록 저평가")
                .category(FactorCategory.VALUE)
                .direction(FactorDirection.LOWER_BETTER)
                .defaultWeight(new BigDecimal("0.10"))
                .requiredFields(List.of("pbr"))
                .build();
    }

    public static FactorDefinition per() {
        return FactorDefinition.builder()
                .code("PER")
                .name("주가수익비율")
                .description("주가 / 주당순이익. 낮을수록 저평가")
                .category(FactorCategory.VALUE)
                .direction(FactorDirection.LOWER_BETTER)
                .defaultWeight(new BigDecimal("0.10"))
                .requiredFields(List.of("per"))
                .build();
    }

    public static FactorDefinition evEbitda() {
        return FactorDefinition.builder()
                .code("EV_EBITDA")
                .name("EV/EBITDA")
                .description("기업가치 / EBITDA. 낮을수록 저평가")
                .category(FactorCategory.VALUE)
                .direction(FactorDirection.LOWER_BETTER)
                .defaultWeight(new BigDecimal("0.08"))
                .requiredFields(List.of("evEbitda"))
                .build();
    }

    public static FactorDefinition momentum3m() {
        return FactorDefinition.builder()
                .code("MOMENTUM_3M")
                .name("3개월 모멘텀")
                .description("최근 3개월 수익률")
                .category(FactorCategory.MOMENTUM)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.12"))
                .requiredFields(List.of("closePrice", "return3m"))
                .build();
    }

    public static FactorDefinition momentum6m() {
        return FactorDefinition.builder()
                .code("MOMENTUM_6M")
                .name("6개월 모멘텀")
                .description("최근 6개월 수익률")
                .category(FactorCategory.MOMENTUM)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.10"))
                .requiredFields(List.of("closePrice", "return6m"))
                .build();
    }

    public static FactorDefinition momentum12m() {
        return FactorDefinition.builder()
                .code("MOMENTUM_12M")
                .name("12개월 모멘텀")
                .description("최근 12개월 수익률")
                .category(FactorCategory.MOMENTUM)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.08"))
                .requiredFields(List.of("closePrice", "return12m"))
                .build();
    }

    public static FactorDefinition roe() {
        return FactorDefinition.builder()
                .code("ROE")
                .name("자기자본이익률")
                .description("순이익 / 자기자본. 높을수록 효율적")
                .category(FactorCategory.QUALITY)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.10"))
                .requiredFields(List.of("roe"))
                .build();
    }

    public static FactorDefinition operatingMargin() {
        return FactorDefinition.builder()
                .code("OPERATING_MARGIN")
                .name("영업이익률")
                .description("영업이익 / 매출액. 높을수록 수익성 양호")
                .category(FactorCategory.QUALITY)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.08"))
                .requiredFields(List.of("operatingMarginPct"))
                .build();
    }

    public static FactorDefinition debtRatio() {
        return FactorDefinition.builder()
                .code("DEBT_RATIO")
                .name("부채비율")
                .description("총부채 / 자기자본. 낮을수록 안정적")
                .category(FactorCategory.QUALITY)
                .direction(FactorDirection.LOWER_BETTER)
                .defaultWeight(new BigDecimal("0.06"))
                .requiredFields(List.of("debtRatio"))
                .build();
    }

    public static FactorDefinition marketCap() {
        return FactorDefinition.builder()
                .code("MARKET_CAP")
                .name("시가총액")
                .description("시가총액 (억원)")
                .category(FactorCategory.SIZE)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.04"))
                .requiredFields(List.of("marketCap"))
                .build();
    }

    public static FactorDefinition volatility() {
        return FactorDefinition.builder()
                .code("VOLATILITY")
                .name("변동성")
                .description("최근 N일 수익률 표준편차. 낮을수록 안정적")
                .category(FactorCategory.VOLATILITY)
                .direction(FactorDirection.LOWER_BETTER)
                .defaultWeight(new BigDecimal("0.06"))
                .requiredFields(List.of("dailyReturns"))
                .parameters(Map.of("window", 20))
                .build();
    }

    public static FactorDefinition beta() {
        return FactorDefinition.builder()
                .code("BETA")
                .name("베타")
                .description("시장 대비 민감도. 1 이하이면 방어적")
                .category(FactorCategory.VOLATILITY)
                .direction(FactorDirection.LOWER_BETTER)
                .defaultWeight(new BigDecimal("0.04"))
                .requiredFields(List.of("beta"))
                .build();
    }

    public static FactorDefinition disparity() {
        return FactorDefinition.builder()
                .code("DISPARITY")
                .name("이격도")
                .description("현재가 / 이동평균 * 100. 기존 FactorCalculationService 연동")
                .category(FactorCategory.TECHNICAL)
                .direction(FactorDirection.NEUTRAL)
                .defaultWeight(new BigDecimal("0.08"))
                .requiredFields(List.of("closePrice", "ma20"))
                .build();
    }

    public static FactorDefinition volatilityBreakout() {
        return FactorDefinition.builder()
                .code("VOLATILITY_BREAKOUT")
                .name("변동성 돌파")
                .description("목표가 = 시가 + (전일 레인지 * k). 기존 FactorCalculationService 연동")
                .category(FactorCategory.TECHNICAL)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.06"))
                .requiredFields(List.of("openPrice", "prevHighPrice", "prevLowPrice"))
                .build();
    }

    public static FactorDefinition smartMoneyIntensity() {
        return FactorDefinition.builder()
                .code("SMART_MONEY_INTENSITY")
                .name("수급 강도")
                .description("5일 누적 순매수 / 시총. 기존 FactorCalculationService 연동")
                .category(FactorCategory.FLOW)
                .direction(FactorDirection.HIGHER_BETTER)
                .defaultWeight(new BigDecimal("0.10"))
                .requiredFields(List.of("netBuyAmt5d", "marketCap"))
                .build();
    }
}
