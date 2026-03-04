package com.investment.factor.service;

import com.investment.config.RiskProperties;
import com.investment.risk.dto.MacroDashboardResponse;
import com.investment.risk.service.RegimeDetectionService;
import com.investment.setting.service.SystemSettingService;
import com.investment.strategy.engine.MacroEconomicStrategyEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 리스크 게이트 — 파이프라인 실행 전 시장 레짐·VIX 확인.
 * 고변동성 시 신규 매수 비중 축소 또는 스킵 (전문 투자자 흐름 P0).
 * 활성 여부는 Admin 시스템 설정(risk.regimeGateEnabled)에서 조회.
 * RegimeDetectionService가 활성화되면 VIX/이평선 기반 BEAR 시 비중 축소 적용.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskGateService {

    private final RiskProperties riskProperties;
    private final MacroEconomicStrategyEngine macroEconomicStrategyEngine;
    private final SystemSettingService systemSettingService;
    private final RegimeDetectionService regimeDetectionService;

    private boolean isRegimeGateEnabled() {
        return Boolean.TRUE.equals(systemSettingService.getBoolean("risk.regimeGateEnabled"));
    }

    /**
     * 현재 레짐·VIX(선택) 기준으로 신규 매수 허용 여부와 비중 배율 반환.
     * 레짐 탐지 활성 시 BEAR 레짐이면 비중 축소. 그 외 VIX 기준 적용.
     *
     * @param vix VIX 값 (null이면 레짐 게이트 미적용 시 허용)
     * @return allowNewBuy, sizeMultiplier (0~1)
     */
    public RiskGateResult evaluate(BigDecimal vix) {
        if (!isRegimeGateEnabled()) {
            return RiskGateResult.allow(BigDecimal.ONE);
        }
        if (riskProperties.isRegimeDetectionEnabled()) {
            RegimeDetectionService.RegimeResult regimeResult = regimeDetectionService.getCurrentRegime(null);
            if (regimeResult.regime() == MacroDashboardResponse.MarketRegime.BEAR) {
                BigDecimal reducePct = riskProperties.getReduceSizeOnHighVolPct();
                if (reducePct == null) {
                    reducePct = new BigDecimal("50");
                }
                BigDecimal multiplier = BigDecimal.valueOf(100).subtract(reducePct)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                log.info("리스크 게이트: 레짐=BEAR → 신규 매수 비중 {}% 적용", reducePct);
                return RiskGateResult.allow(multiplier);
            }
        }
        if (vix == null) {
            log.debug("리스크 게이트: VIX 미제공, 신규 매수 허용(배율 1.0)");
            return RiskGateResult.allow(BigDecimal.ONE);
        }
        BigDecimal threshold = riskProperties.getVixThreshold();
        if (threshold == null) {
            threshold = new BigDecimal("30");
        }
        if (vix.compareTo(threshold) > 0) {
            BigDecimal reducePct = riskProperties.getReduceSizeOnHighVolPct();
            if (reducePct == null || reducePct.compareTo(BigDecimal.ZERO) <= 0) {
                reducePct = new BigDecimal("50");
            }
            BigDecimal multiplier = BigDecimal.valueOf(100).subtract(reducePct)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            log.info("리스크 게이트: VIX={} > {} → 고변동성, 신규 매수 비중 {}% 적용", vix, threshold, reducePct);
            return RiskGateResult.allow(multiplier);
        }
        return RiskGateResult.allow(BigDecimal.ONE);
    }

    /**
     * MacroEconomicStrategyEngine으로 레짐 판단 후 결과 반환.
     * 지표 미제공 시 허용 + 1.0.
     */
    public RiskGateResult evaluateWithIndicators(MacroEconomicStrategyEngine.MacroEconomicIndicators indicators) {
        if (!isRegimeGateEnabled() || indicators == null) {
            return RiskGateResult.allow(BigDecimal.ONE);
        }
        try {
            MacroEconomicStrategyEngine.InvestmentStrategy strategy = macroEconomicStrategyEngine
                    .decideStrategy(indicators);
            if (strategy.getRegime() == MacroEconomicStrategyEngine.MarketRegime.HIGH_VOLATILITY) {
                BigDecimal reducePct = riskProperties.getReduceSizeOnHighVolPct();
                if (reducePct == null) {
                    reducePct = new BigDecimal("50");
                }
                BigDecimal multiplier = BigDecimal.valueOf(100).subtract(reducePct)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                log.info("리스크 게이트: 레짐={} → 고변동성, 신규 매수 비중 {}% 적용", strategy.getRegime(), reducePct);
                return RiskGateResult.allow(multiplier);
            }
        } catch (Exception e) {
            log.warn("리스크 게이트 레짐 판단 실패, 허용 처리: {}", e.getMessage());
        }
        return RiskGateResult.allow(BigDecimal.ONE);
    }

    /**
     * 드로다운 회복 모드 여부 (P6-1).
     * maxMddPct가 drawdown-recovery-threshold-pct(기본 10%) 이상이면 true.
     * drawdown-recovery-exit-pct(기본 5%) 이하로 회복되면 false.
     * (maxMddPct는 호출부에서 RiskReportService.getMaxMddPctForUser 등으로 조회)
     */
    public boolean isDrawdownRecoveryMode(BigDecimal maxMddPct) {
        if (maxMddPct == null) {
            return false;
        }
        BigDecimal threshold = riskProperties.getDrawdownRecoveryThresholdPct();
        BigDecimal exitPct = riskProperties.getDrawdownRecoveryExitPct();
        if (threshold == null) {
            threshold = new BigDecimal("0.10");
        }
        if (exitPct == null) {
            exitPct = new BigDecimal("0.05");
        }
        if (maxMddPct.compareTo(exitPct) <= 0) {
            return false;
        }
        return maxMddPct.compareTo(threshold) >= 0;
    }

    /** 드로다운 회복 모드 시 신규 매수 권장 금액 스케일 (0~1). 기본 0.5 */
    public BigDecimal getDrawdownRecoveryScale() {
        BigDecimal scale = riskProperties.getDrawdownRecoveryScale();
        return scale != null && scale.compareTo(BigDecimal.ZERO) > 0 ? scale : new BigDecimal("0.5");
    }

    @lombok.Getter
    @lombok.Builder
    @lombok.AllArgsConstructor
    public static class RiskGateResult {
        private final boolean allowNewBuy;
        private final BigDecimal sizeMultiplier;

        public static RiskGateResult allow(BigDecimal sizeMultiplier) {
            return new RiskGateResult(true, sizeMultiplier != null ? sizeMultiplier : BigDecimal.ONE);
        }

        /** 신규 매수 불가 시 사용 (리스크 게이트 차단). */
        public static RiskGateResult disallow() {
            return new RiskGateResult(false, BigDecimal.ONE);
        }
    }
}
