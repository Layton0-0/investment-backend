package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.OnboardingProfileResponseDto;
import com.investment.setting.dto.OnboardingQuizRequestDto;
import com.investment.setting.dto.TradingSettingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 초보자 온보딩: 3문항 퀴즈 → 프로필(보수/균형/공격) 및 단기/중기/장기 비율 산출.
 * applyToSettings true 시 TradingSetting에 비율 반영.
 * 빈 이름을 명시해 com.investment.onboarding.OnboardingService 등 동일 이름 빈과의 충돌을 방지.
 */
@Service("settingOnboardingService")
@RequiredArgsConstructor
public class OnboardingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal R_02 = new BigDecimal("0.2");
    private static final BigDecimal R_03 = new BigDecimal("0.3");
    private static final BigDecimal R_04 = new BigDecimal("0.4");
    private static final BigDecimal R_08 = new BigDecimal("0.8");

    private final TradingSettingRepository tradingSettingRepository;
    private final TradingSettingService tradingSettingService;

    /**
     * 퀴즈 제출 (컨트롤러에서 userId 전달용).
     * 인증된 사용자 이름을 userId로 사용.
     */
    @Transactional
    public OnboardingProfileResponseDto submitQuiz(String userId, OnboardingQuizRequestDto request) {
        return submitProfile(userId, request);
    }

    /**
     * 퀴즈 응답으로 프로필·비율 산출 및 선택 시 설정 반영.
     *
     * @param userId  사용자 ID (인증된 사용자명)
     * @param request 퀴즈 요청 (investmentHorizon, riskTolerance, investmentAmount, applyToSettings)
     */
    @Transactional
    public OnboardingProfileResponseDto submitProfile(String userId, OnboardingQuizRequestDto request) {
        if (request == null || request.getInvestmentHorizon() == null || request.getInvestmentHorizon().isBlank()) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "투자 기간을 선택해 주세요.");
        }
        if (request.getRiskTolerance() == null || request.getRiskTolerance().isBlank()) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "위험 감수를 선택해 주세요.");
        }
        if (request.getInvestmentAmount() == null || request.getInvestmentAmount().isBlank()) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "투자 금액을 선택해 주세요.");
        }

        String profile = resolveProfile(request.getInvestmentHorizon(), request.getRiskTolerance(), request.getInvestmentAmount());
        BigDecimal shortR;
        BigDecimal mediumR;
        BigDecimal longR;
        switch (profile) {
            case "CONSERVATIVE":
                shortR = ZERO;
                mediumR = R_02;
                longR = R_08;
                break;
            case "AGGRESSIVE":
                shortR = R_03;
                mediumR = R_04;
                longR = R_03;
                break;
            default:
                // BALANCED
                shortR = R_02;
                mediumR = R_04;
                longR = R_04;
                break;
        }

        boolean appliedToSettings = false;
        if (Boolean.TRUE.equals(request.getApplyToSettings()) && userId != null && !userId.isBlank()) {
            List<TradingSetting> settings = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
            if (!settings.isEmpty()) {
                TradingSetting first = settings.get(0);
                String accountNo = first.getAccountNo();
                Optional<TradingSettingDto> existing = tradingSettingService.getSettingOptional(accountNo);
                if (existing.isPresent()) {
                    TradingSettingDto toSave = existing.get();
                    TradingSettingDto updated = TradingSettingDto.builder()
                            .maxInvestmentAmount(toSave.getMaxInvestmentAmount())
                            .minInvestmentAmount(toSave.getMinInvestmentAmount())
                            .defaultCurrency(toSave.getDefaultCurrency() != null ? toSave.getDefaultCurrency() : "KRW")
                            .autoTradingEnabled(toSave.getAutoTradingEnabled())
                            .roboAdvisorEnabled(toSave.getRoboAdvisorEnabled() != null && toSave.getRoboAdvisorEnabled())
                            .riskLevel(toSave.getRiskLevel())
                            .shortTermRatio(shortR)
                            .mediumTermRatio(mediumR)
                            .longTermRatio(longR)
                            .pipelineAutoExecute(toSave.getPipelineAutoExecute())
                            .pipelineAllowRealExecution(toSave.getPipelineAllowRealExecution())
                            .build();
                    tradingSettingService.saveSetting(accountNo, updated);
                    appliedToSettings = true;
                }
            }
        }

        return OnboardingProfileResponseDto.builder()
                .profile(profile)
                .shortTermRatio(shortR)
                .mediumTermRatio(mediumR)
                .longTermRatio(longR)
                .appliedToSettings(appliedToSettings)
                .build();
    }

    private String resolveProfile(String horizon, String risk, String amount) {
        if ("3Y_PLUS".equals(horizon) && "N5".equals(risk)) {
            return "CONSERVATIVE";
        }
        if ("3M".equals(horizon) && "N20".equals(risk)) {
            return "AGGRESSIVE";
        }
        return "BALANCED";
    }
}
