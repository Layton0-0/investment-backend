package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.setting.dto.OnboardingProfileResponseDto;
import com.investment.setting.dto.OnboardingQuizRequestDto;
import com.investment.setting.dto.TradingSettingDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OnboardingService")
class OnboardingServiceTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;

    @Mock
    private TradingSettingService tradingSettingService;

    @InjectMocks
    private OnboardingService onboardingService;

    @Test
    @DisplayName("3Y_PLUS + N5 → CONSERVATIVE, 비율 0/0.2/0.8")
    void submitProfile_conservative_returnsCorrectRatios() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("3Y_PLUS")
                .riskTolerance("N5")
                .investmentAmount("1M")
                .applyToSettings(false)
                .build();

        OnboardingProfileResponseDto result = onboardingService.submitProfile("user-1", request);

        assertThat(result.getProfile()).isEqualTo("CONSERVATIVE");
        assertThat(result.getShortTermRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getMediumTermRatio()).isEqualByComparingTo(new BigDecimal("0.2"));
        assertThat(result.getLongTermRatio()).isEqualByComparingTo(new BigDecimal("0.8"));
        assertThat(result.getAppliedToSettings()).isFalse();
    }

    @Test
    @DisplayName("3M + N20 → AGGRESSIVE, 비율 0.3/0.4/0.3")
    void submitProfile_aggressive_returnsCorrectRatios() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("3M")
                .riskTolerance("N20")
                .investmentAmount("10M_PLUS")
                .applyToSettings(false)
                .build();

        OnboardingProfileResponseDto result = onboardingService.submitProfile("user-1", request);

        assertThat(result.getProfile()).isEqualTo("AGGRESSIVE");
        assertThat(result.getShortTermRatio()).isEqualByComparingTo(new BigDecimal("0.3"));
        assertThat(result.getMediumTermRatio()).isEqualByComparingTo(new BigDecimal("0.4"));
        assertThat(result.getLongTermRatio()).isEqualByComparingTo(new BigDecimal("0.3"));
    }

    @Test
    @DisplayName("1Y + N10 → BALANCED, 비율 0.2/0.4/0.4")
    void submitProfile_balanced_returnsCorrectRatios() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("1Y")
                .riskTolerance("N10")
                .investmentAmount("5M")
                .applyToSettings(false)
                .build();

        OnboardingProfileResponseDto result = onboardingService.submitProfile("user-1", request);

        assertThat(result.getProfile()).isEqualTo("BALANCED");
        assertThat(result.getShortTermRatio()).isEqualByComparingTo(new BigDecimal("0.2"));
        assertThat(result.getMediumTermRatio()).isEqualByComparingTo(new BigDecimal("0.4"));
        assertThat(result.getLongTermRatio()).isEqualByComparingTo(new BigDecimal("0.4"));
    }

    @Test
    @DisplayName("applyToSettings true + 계좌 있으면 saveSetting 호출")
    void submitProfile_applyToSettings_true_callsSaveSetting() {
        TradingSetting setting = TradingSetting.builder()
                .accountNo("12345678-01")
                .userId("user-1")
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(new BigDecimal("100000"))
                .defaultCurrency("KRW")
                .autoTradingEnabled(false)
                .roboAdvisorEnabled(false)
                .shortTermRatio(new BigDecimal("0.2"))
                .mediumTermRatio(new BigDecimal("0.4"))
                .longTermRatio(new BigDecimal("0.4"))
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user-1")).thenReturn(List.of(setting));
        when(tradingSettingService.getSettingOptional("12345678-01")).thenReturn(Optional.of(TradingSettingDto.builder()
                .maxInvestmentAmount(new BigDecimal("10000000"))
                .minInvestmentAmount(new BigDecimal("100000"))
                .defaultCurrency("KRW")
                .autoTradingEnabled(false)
                .roboAdvisorEnabled(false)
                .shortTermRatio(new BigDecimal("0.2"))
                .mediumTermRatio(new BigDecimal("0.4"))
                .longTermRatio(new BigDecimal("0.4"))
                .build()));
        when(tradingSettingService.saveSetting(eq("12345678-01"), any(TradingSettingDto.class))).thenReturn(null);

        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("3Y_PLUS")
                .riskTolerance("N5")
                .investmentAmount("1M")
                .applyToSettings(true)
                .build();

        OnboardingProfileResponseDto result = onboardingService.submitProfile("user-1", request);

        assertThat(result.getAppliedToSettings()).isTrue();
        ArgumentCaptor<TradingSettingDto> captor = ArgumentCaptor.forClass(TradingSettingDto.class);
        verify(tradingSettingService).saveSetting(eq("12345678-01"), captor.capture());
        assertThat(captor.getValue().getShortTermRatio()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getLongTermRatio()).isEqualByComparingTo(new BigDecimal("0.8"));
    }

    @Test
    @DisplayName("퀴즈 항목 null이면 DomainException")
    void submitProfile_nullHorizon_throws() {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon(null)
                .riskTolerance("N10")
                .investmentAmount("5M")
                .build();

        assertThatThrownBy(() -> onboardingService.submitProfile("user-1", request))
                .isInstanceOf(DomainException.class);
    }
}
