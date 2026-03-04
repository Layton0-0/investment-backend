package com.investment.onboarding;

import com.investment.api.dto.OnboardingProfileResponseDto;
import com.investment.api.dto.OnboardingQuizRequestDto;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 초보자 온보딩 퀴즈 → 프로필·전략 비율 산출 및 TradingSetting 반영.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private static final String CONSERVATIVE = "CONSERVATIVE";
    private static final String BALANCED = "BALANCED";
    private static final String AGGRESSIVE = "AGGRESSIVE";

    private final TradingSettingRepository tradingSettingRepository;

    /**
     * 퀴즈 응답으로 프로필 및 단기/중기/장기 비율 계산.
     * applyToSettings true이고 인증된 사용자면 해당 계좌 TradingSetting에 비율 저장.
     */
    @Transactional
    public OnboardingProfileResponseDto submitQuiz(OnboardingQuizRequestDto request) {
        String profile = computeProfile(request.getInvestmentHorizon(), request.getRiskTolerance(), request.getInvestmentAmount());
        BigDecimal shortR = shortTermRatio(profile);
        BigDecimal mediumR = mediumTermRatio(profile);
        BigDecimal longR = longTermRatio(profile);

        boolean applied = false;
        if (Boolean.TRUE.equals(request.getApplyToSettings())) {
            applied = applyToUserSetting(request.getAccountNo(), shortR, mediumR, longR);
        }

        return OnboardingProfileResponseDto.builder()
                .profile(profile)
                .shortTermRatio(shortR)
                .mediumTermRatio(mediumR)
                .longTermRatio(longR)
                .appliedToSettings(applied)
                .build();
    }

    private String computeProfile(String horizon, String risk, String amount) {
        int h = points(horizon, "3M", 0, "1Y", 1, "3Y_PLUS", 2);
        int r = points(risk, "N5", 0, "N10", 1, "N20", 2);
        int a = points(amount, "1M", 0, "5M", 1, "10M_PLUS", 2);
        int total = h + r + a;
        if (total <= 2) return CONSERVATIVE;
        if (total <= 4) return BALANCED;
        return AGGRESSIVE;
    }

    private static int points(String value, String v0, int p0, String v1, int p1, String v2, int p2) {
        if (value == null) return 0;
        if (value.equalsIgnoreCase(v0)) return p0;
        if (value.equalsIgnoreCase(v1)) return p1;
        if (value.equalsIgnoreCase(v2)) return p2;
        return 0;
    }

    private static BigDecimal shortTermRatio(String profile) {
        return switch (profile) {
            case CONSERVATIVE -> BigDecimal.ZERO;
            case BALANCED -> new BigDecimal("0.20");
            case AGGRESSIVE -> new BigDecimal("0.30");
            default -> new BigDecimal("0.20");
        };
    }

    private static BigDecimal mediumTermRatio(String profile) {
        return switch (profile) {
            case CONSERVATIVE -> new BigDecimal("0.20");
            case BALANCED -> new BigDecimal("0.40");
            case AGGRESSIVE -> new BigDecimal("0.40");
            default -> new BigDecimal("0.40");
        };
    }

    private static BigDecimal longTermRatio(String profile) {
        return switch (profile) {
            case CONSERVATIVE -> new BigDecimal("0.80");
            case BALANCED -> new BigDecimal("0.40");
            case AGGRESSIVE -> new BigDecimal("0.30");
            default -> new BigDecimal("0.40");
        };
    }

    private boolean applyToUserSetting(String accountNo, BigDecimal shortR, BigDecimal mediumR, BigDecimal longR) {
        String userId = getCurrentUserId();
        if (userId == null) return false;

        TradingSetting setting;
        if (accountNo != null && !accountNo.isBlank()) {
            setting = tradingSettingRepository.findByAccountNo(accountNo).orElse(null);
        } else {
            List<TradingSetting> list = tradingSettingRepository.findByUserIdOrderByAccountNo(userId);
            setting = list.isEmpty() ? null : list.get(0);
        }
        if (setting == null) {
            log.debug("온보딩 설정 적용 스킵: 계좌 없음, userId={}", userId);
            return false;
        }
        setting.updateStrategyRatios(shortR, mediumR, longR);
        tradingSettingRepository.save(setting);
        log.info("온보딩 프로필 적용: accountNo={}, short={}, medium={}, long={}", setting.getAccountNo(), shortR, mediumR, longR);
        return true;
    }

    private static String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return auth.getName();
    }
}
