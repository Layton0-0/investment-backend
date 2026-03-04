package com.investment.onboarding;

import com.investment.api.dto.OnboardingProfileResponseDto;
import com.investment.api.dto.OnboardingQuizRequestDto;
import com.investment.domain.repository.TradingSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService")
class OnboardingServiceTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    @DisplayName("퀴즈 제출 시 보수적 프로필 반환")
    void submitQuiz_conservative_returnsProfile() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("3M")
                .riskTolerance("N5")
                .investmentAmount("1M")
                .applyToSettings(false)
                .build();

        OnboardingProfileResponseDto response = onboardingService.submitQuiz(request);

        assertThat(response.getProfile()).isEqualTo("CONSERVATIVE");
        assertThat(response.getShortTermRatio()).isEqualByComparingTo("0");
        assertThat(response.getMediumTermRatio()).isEqualByComparingTo("0.2");
        assertThat(response.getLongTermRatio()).isEqualByComparingTo("0.8");
        verify(tradingSettingRepository, never()).save(any());
    }

    @Test
    @DisplayName("퀴즈 제출 시 균형 프로필 반환")
    void submitQuiz_balanced_returnsProfile() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("1Y")
                .riskTolerance("N10")
                .investmentAmount("5M")
                .build();

        OnboardingProfileResponseDto response = onboardingService.submitQuiz(request);

        assertThat(response.getProfile()).isEqualTo("BALANCED");
        assertThat(response.getShortTermRatio()).isEqualByComparingTo("0.2");
        assertThat(response.getMediumTermRatio()).isEqualByComparingTo("0.4");
        assertThat(response.getLongTermRatio()).isEqualByComparingTo("0.4");
    }

    @Test
    @DisplayName("퀴즈 제출 시 공격적 프로필 반환")
    void submitQuiz_aggressive_returnsProfile() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("3Y_PLUS")
                .riskTolerance("N20")
                .investmentAmount("10M_PLUS")
                .build();

        OnboardingProfileResponseDto response = onboardingService.submitQuiz(request);

        assertThat(response.getProfile()).isEqualTo("AGGRESSIVE");
        assertThat(response.getShortTermRatio()).isEqualByComparingTo("0.3");
        assertThat(response.getMediumTermRatio()).isEqualByComparingTo("0.4");
        assertThat(response.getLongTermRatio()).isEqualByComparingTo("0.3");
    }
}
