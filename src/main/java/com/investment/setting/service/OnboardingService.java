package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.OnboardingProfileResponseDto;
import com.investment.setting.dto.OnboardingQuizRequestDto;
import com.investment.setting.dto.TradingSettingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 초보자 온보딩: 3문항 퀴즈 → 프로필(보수/균형/공격) 및 전략 비율 산출, 선택 시 TradingSetting 반영.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final BigDecimal SHORT_CONSERVATIVE = BigDecimal.ZERO;
    private static final BigDecimal MID_CONSERVATIVE = new BigDecimal("0.2");
    private static final BigDecimal LONG_CONSERVATIVE = new BigDecimal("0.8");

    private static final BigDecimal SHORT_BALANCED = new BigDecimal("0.2");
    private static final BigDecimal MID_BALANCED = new BigDecimal("0.4");
    private static final BigDecimal LONG_BALANCED = new BigDecimal("0.4");

    private static final BigDecimal SHORT_AGGRESSIVE = new BigDecimal("0.3");
    private static final BigDecimal MID_AGGRESSIVE = new BigDecimal("0.4");
    private static final BigDecimal LONG_AGGRESSIVE = new BigDecimal("0.3");

    private final TradingSettingRepository tradingSettingRepository;
    private final TradingSettingService tradingSettingService;

    /**
     * 퀴즈 응답으로 프로필·비율 산출 후, applyToSettings true면 해당 사용자 계좌 설정에 반영.
     *
     * @param userId 인증 사용자 ID
     * @param request 퀴즈 요청
     * @return 프로필 및 비율, appliedToSettings 반영 여부
     */
    @Transactional
    public OnboardingProfileResponseDto submitProfile(String userId, OnboardingQuizRequestDto request) {
        String profile = resolveProfile(
                request.getInvestmentHorizon(),
                request.getRiskTolerance(),
                request.getInvestmentAmount());
        BigDecimal shortR = getShortRatio(profile);
        BigDecimal midR = getMediumRatio(profile);
        BigDecimal longR = getLongRatio(profile);

        boolean applied = false;
        if (Boolean.TRUE.equals(request.getApplyToSettings())) {
            String accountNo = resolveAccountNo(userId, request.getAccountNo());
            if (accountNo != null) {
                applied = applyRatiosToSetting(accountNo, shortR, midR, longR);
            }
        }

        return OnboardingProfileResponseDto.builder()
                .profile(profile)
                .shortTermRatio(shortR.setScale(4, RoundingMode.HALF_UP))
                .mediumTermRatio(midR.setScale(4, RoundingMode.HALF_UP))
                .longTermRatio(longR.setScale(4, RoundingMode.HALF_UP))
                .appliedToSettings(applied)
                .build();
    }

    private String resolveProfile(String horizon, String risk, String amount) {
        if (horizon == null || risk == null || amount == null) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE, "퀴즈 항목이 비어 있습니다");
        }
        boolean longTerm = "3Y_PLUS".equals(horizon);
        boolean lowRisk = "N5".equals(risk);
        boolean shortTerm = "3M".equals(horizon);
        boolean highRisk = "N20".equals(risk);
        if (longTerm && lowRisk) {
            return "CONSERVATIVE";
        }
        if (shortTerm && highRisk) {
            return "AGGRESSIVE";
        }
        return "BALANCED";
    }

    private BigDecimal getShortRatio(String profile) {
        return switch (profile) {
            case "CONSERVATIVE" -> SHORT_CONSERVATIVE;
            case "AGGRESSIVE" -> SHORT_AGGRESSIVE;
            default -> SHORT_BALANCED;
        };
    }

    private BigDecimal getMediumRatio(String profile) {
        return switch (profile) {
            case "CONSERVATIVE" -> MID_CONSERVATIVE;
            case "AGGRESSIVE" -> MID_AGGRESSIVE;
            default -> MID_BALANCED;
        };
    }

    private BigDecimal getLongRatio(String profile) {
        return switch (profile) {
            case "CONSERVATIVE" -> LONG_CONSERVATIVE;
            case "AGGRESSIVE" -> LONG_AGGRESSIVE;
            default -> LONG_BALANCED;
        };
    }

    private String resolveAccountNo(String userId, String requestAccountNo) {
        if (requestAccountNo != null && !requestAccountNo.isBlank()) {
            tradingSettingRepository.findByAccountNo(requestAccountNo)
                    .filter(s -> userId.equals(s.getUserId()))
                    .orElseThrow(() -> new DomainException(ErrorCode.SETTING_NOT_FOUND,
                            "해당 계좌 설정을 찾을 수 없습니다"));
            return requestAccountNo.trim();
        }
        List<TradingSetting> list = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
        return list.isEmpty() ? null : list.get(0).getAccountNo();
    }

    private boolean applyRatiosToSetting(String accountNo, BigDecimal shortR, BigDecimal midR, BigDecimal longR) {
        return tradingSettingService.getSettingOptional(accountNo)
                .map(existing -> {
                    TradingSettingDto dto = TradingSettingDto.builder()
                            .maxInvestmentAmount(existing.getMaxInvestmentAmount())
                            .minInvestmentAmount(existing.getMinInvestmentAmount())
                            .defaultCurrency(existing.getDefaultCurrency())
                            .autoTradingEnabled(existing.getAutoTradingEnabled())
                            .roboAdvisorEnabled(existing.getRoboAdvisorEnabled())
                            .riskLevel(existing.getRiskLevel())
                            .shortTermRatio(shortR)
                            .mediumTermRatio(midR)
                            .longTermRatio(longR)
                            .pipelineAutoExecute(existing.getPipelineAutoExecute())
                            .pipelineAllowRealExecution(existing.getPipelineAllowRealExecution())
                            .build();
                    tradingSettingService.saveSetting(accountNo, dto);
                    return true;
                })
                .orElse(false);
    }
}
