package com.investment.risk.service;

import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.config.RiskProperties;
import com.investment.domain.entity.UserAccount;
import com.investment.setting.service.SystemSettingService;
import com.investment.domain.repository.PortfolioPeakRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.RiskGateService;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.PortfolioRiskMetricsDto;
import com.investment.risk.dto.RiskAccountSummaryDto;
import com.investment.risk.dto.RiskHistoryItemDto;
import com.investment.risk.dto.RiskLimitsDto;
import com.investment.risk.dto.RiskSummaryDto;
import com.investment.risk.util.VarCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 리스크 리포트 집계 서비스.
 * 킬스위치·일일 손실 한도·리스크 게이트·계좌별 MDD 등을 조합해 API용 DTO를 반환한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskReportService {

    private final TradingHaltService tradingHaltService;
    private final RiskGateService riskGateService;
    private final DailyLossLimitService dailyLossLimitService;
    private final PortfolioPeakRepository portfolioPeakRepository;
    private final RiskProperties riskProperties;
    private final SystemSettingService systemSettingService;
    private final TradingSettingRepository tradingSettingRepository;
    private final UserAccountRepository userAccountRepository;
    private final EncryptionUtil encryptionUtil;
    private final VarCalculator varCalculator;

    /**
     * 현재 사용자 기준 리스크 요약.
     * 킬스위치·리스크 게이트·계좌별 일일 손실 한도·MDD.
     */
    @Transactional(readOnly = true)
    public RiskSummaryDto getSummary(String userId) {
        boolean killSwitch = tradingHaltService.isHaltAllOrders();
        RiskGateService.RiskGateResult gateResult = riskGateService.evaluate(null);
        List<RiskAccountSummaryDto> accounts = getAccountSummaries(userId);
        BigDecimal totalCurrentValue = accounts.stream()
                .map(RiskAccountSummaryDto::getCurrentValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal maxMddPct = accounts.stream()
                .map(RiskAccountSummaryDto::getMdd)
                .filter(v -> v != null)
                .max(BigDecimal::compareTo)
                .orElse(null);
        BigDecimal var95Pct = varCalculator.calculateVar95(null);
        BigDecimal cvar95Pct = varCalculator.calculateCvar95(null);
        String riskLevel = deriveRiskLevel(maxMddPct, var95Pct);
        return RiskSummaryDto.builder()
                .killSwitchActive(killSwitch)
                .regimeGateEnabled(Boolean.TRUE.equals(systemSettingService.getBoolean("risk.regimeGateEnabled")))
                .riskGateAllowsNewBuy(gateResult.isAllowNewBuy())
                .riskGateSizeMultiplier(gateResult.getSizeMultiplier())
                .accounts(accounts)
                .totalCurrentValue(totalCurrentValue)
                .maxMddPct(maxMddPct)
                .var95Pct(var95Pct)
                .cvar95Pct(cvar95Pct)
                .sharpeRatio(null)
                .sortinoRatio(null)
                .riskLevel(riskLevel)
                .build();
    }

    /**
     * VaR·MDD 기반 리스크 수준 문자열.
     * MDD 20% 이상 또는 VaR 5% 이상 → 높음, MDD 10% 이상 또는 VaR 2% 이상 → 중간, 그 외 → 낮음.
     */
    private static String deriveRiskLevel(BigDecimal maxMddPct, BigDecimal var95Pct) {
        double mdd = maxMddPct != null ? maxMddPct.doubleValue() : 0d;
        double varPct = var95Pct != null ? var95Pct.doubleValue() : 0d;
        if (mdd >= 0.20 || varPct >= 5.0) {
            return "높음";
        }
        if (mdd >= 0.10 || varPct >= 2.0) {
            return "중간";
        }
        return "낮음";
    }

    /**
     * 단일 계좌 포트폴리오 리스크 메트릭 (VaR/CVaR/Sharpe/Sortino).
     * 해당 계좌가 사용자 소유가 아니면 null 반환.
     */
    @Transactional(readOnly = true)
    public PortfolioRiskMetricsDto getPortfolioRiskMetrics(String userId, String accountNo) {
        if (userId == null || accountNo == null) {
            return null;
        }
        List<com.investment.domain.entity.TradingSetting> settings = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
        com.investment.domain.entity.TradingSetting setting = settings.stream()
                .filter(s -> accountNo.equals(s.getAccountNo()))
                .findFirst()
                .orElse(null);
        if (setting == null) {
            return null;
        }
        String accNo = setting.getAccountNo();
        LocalDate today = LocalDate.now();
        BigDecimal opening = dailyLossLimitService.getOpeningBalance(accNo, today);
        BigDecimal current = dailyLossLimitService.getCurrentPortfolioValue(accNo);
        BigDecimal mdd = null;
        Optional<com.investment.domain.entity.PortfolioPeak> peakOpt = portfolioPeakRepository.findByAccountNo(accNo);
        if (peakOpt.isPresent() && current != null && current.compareTo(BigDecimal.ZERO) > 0) {
            com.investment.domain.entity.PortfolioPeak peak = peakOpt.get();
            BigDecimal peakVal = peak.getPeakValue();
            if (peakVal != null && peakVal.compareTo(BigDecimal.ZERO) > 0) {
                mdd = peakVal.subtract(current).divide(peakVal, 6, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
            }
        }
        BigDecimal var95Pct = varCalculator.calculateVar95(null);
        BigDecimal cvar95Pct = varCalculator.calculateCvar95(null);
        return PortfolioRiskMetricsDto.builder()
                .accountNoMasked(LogMaskingUtil.maskAccountNo(accNo))
                .currentValue(current)
                .mddPct(mdd)
                .var95Pct(var95Pct)
                .cvar95Pct(cvar95Pct)
                .varMethod(varCalculator.getCurrentMethod())
                .sharpeRatio(null)
                .sortinoRatio(null)
                .build();
    }

    /**
     * 사용자 계좌들 중 최대 MDD (0~1). 드로다운 회복 모드 판단용(P6-1).
     * 계좌 없거나 MDD 없으면 null.
     */
    @Transactional(readOnly = true)
    public BigDecimal getMaxMddPctForUser(String userId) {
        if (userId == null) {
            return null;
        }
        List<RiskAccountSummaryDto> list = getAccountSummaries(userId);
        return list.stream()
                .map(RiskAccountSummaryDto::getMdd)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);
    }

    /**
     * 리스크 한도 설정 요약 (RiskProperties 기반).
     */
    public RiskLimitsDto getLimits() {
        return RiskLimitsDto.builder()
                .regimeGateEnabled(Boolean.TRUE.equals(systemSettingService.getBoolean("risk.regimeGateEnabled")))
                .vixThreshold(riskProperties.getVixThreshold() != null ? riskProperties.getVixThreshold() : new BigDecimal("30"))
                .reduceSizeOnHighVolPct(riskProperties.getReduceSizeOnHighVolPct() != null ? riskProperties.getReduceSizeOnHighVolPct() : new BigDecimal("50"))
                .dailyLossLimitPct(riskProperties.getDailyLossLimitPct() != null ? riskProperties.getDailyLossLimitPct() : new BigDecimal("5"))
                .varMethod(riskProperties.getVarMethod() != null ? riskProperties.getVarMethod().name() : "PARAMETRIC")
                .varLookbackDays(riskProperties.getVarLookbackDays())
                .yearEndLossLimitPct(riskProperties.getYearEndLossLimitPct() != null ? riskProperties.getYearEndLossLimitPct() : new BigDecimal("20"))
                .yearEndAlertThresholdPct(riskProperties.getYearEndAlertThresholdPct() != null ? riskProperties.getYearEndAlertThresholdPct() : new BigDecimal("0.8"))
                .build();
    }

    /**
     * 리스크 이력 (게이트 축소·손실 한도 도달 등).
     * 1차: 저장 구조 없음으로 빈 목록 반환.
     */
    public List<RiskHistoryItemDto> getHistory(String userId, LocalDate from, LocalDate to) {
        return Collections.emptyList();
    }

    private List<RiskAccountSummaryDto> getAccountSummaries(String userId) {
        List<com.investment.domain.entity.TradingSetting> settings = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
        if (settings == null || settings.isEmpty()) {
            return List.of();
        }
        Map<String, String> accountNoToServerType = buildAccountNoToServerType(userId);
        LocalDate today = LocalDate.now();
        List<RiskAccountSummaryDto> list = new ArrayList<>();
        for (com.investment.domain.entity.TradingSetting setting : settings) {
            String accountNo = setting.getAccountNo();
            String serverType = accountNoToServerType.getOrDefault(accountNo, null);
            BigDecimal opening = dailyLossLimitService.getOpeningBalance(accountNo, today);
            BigDecimal current = dailyLossLimitService.getCurrentPortfolioValue(accountNo);
            boolean newBuyBlocked = !dailyLossLimitService.isNewBuyAllowed(accountNo);
            BigDecimal mdd = null;
            BigDecimal peakValue = null;
            Optional<com.investment.domain.entity.PortfolioPeak> peakOpt = portfolioPeakRepository.findByAccountNo(accountNo);
            if (peakOpt.isPresent() && current != null && current.compareTo(BigDecimal.ZERO) > 0) {
                com.investment.domain.entity.PortfolioPeak peak = peakOpt.get();
                BigDecimal peakVal = peak.getPeakValue();
                if (peakVal != null && peakVal.compareTo(BigDecimal.ZERO) > 0) {
                    peakValue = peakVal;
                    mdd = peakVal.subtract(current).divide(peakVal, 6, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
                }
            }
            list.add(RiskAccountSummaryDto.builder()
                    .accountNoMasked(LogMaskingUtil.maskAccountNo(accountNo))
                    .serverType(serverType)
                    .openingBalance(opening)
                    .currentValue(current)
                    .newBuyBlockedByDailyLoss(newBuyBlocked)
                    .mdd(mdd)
                    .peakValue(peakValue)
                    .build());
        }
        return list;
    }

    private Map<String, String> buildAccountNoToServerType(String userId) {
        List<UserAccount> accounts = userAccountRepository.findByUserId(userId);
        if (accounts == null || accounts.isEmpty()) {
            return Map.of();
        }
        return accounts.stream()
                .filter(a -> a.getAccountNoEncrypted() != null)
                .map(a -> {
                    try {
                        String no = encryptionUtil.decrypt(a.getAccountNoEncrypted());
                        return no != null ? Map.entry(no, a.getServerType()) : null;
                    } catch (Exception e) {
                        log.debug("계좌번호 복호화 스킵: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(e -> e != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }
}
