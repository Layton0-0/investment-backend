package com.investment.api.controller;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.setting.dto.OnboardingProfileResponseDto;
import com.investment.setting.dto.OnboardingQuizRequestDto;
import com.investment.setting.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * 초보자 온보딩: 3문항 퀴즈 → 프로필·전략 비율 산출 및 설정 반영.
 */
@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "온보딩", description = "초보자 퀴즈·프로필 API")
public class OnboardingController {

    @Qualifier("settingOnboardingService")
    private final OnboardingService onboardingService;

    @PostMapping("/profile")
    @Operation(summary = "프로필 제출", description = "3문항 퀴즈 응답으로 프로필(보수/균형/공격) 및 전략 비율 산출. applyToSettings true 시 TradingSetting에 반영")
    public ResponseEntity<OnboardingProfileResponseDto> submitProfile(
            Authentication authentication,
            @RequestBody @Valid OnboardingQuizRequestDto request) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            throw new DomainException(ErrorCode.UNAUTHORIZED, "인증되지 않은 사용자입니다");
        }
        OnboardingProfileResponseDto response = onboardingService.submitProfile(auth.getName(), request);
        return ResponseEntity.ok(response);
    }
}
