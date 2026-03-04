package com.investment.api.controller;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.investment.setting.dto.OnboardingProfileResponseDto;
import com.investment.setting.dto.OnboardingQuizRequestDto;
import com.investment.setting.service.OnboardingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OnboardingController")
class OnboardingControllerTest {

    private static final String USER_ID = "user-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OnboardingService onboardingService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    @Test
    @WithMockUser(username = USER_ID)
    @DisplayName("POST /api/v1/onboarding/profile 인증 시 200 및 profile·비율 반환")
    void submitProfile_authenticated_returnsOkWithProfile() throws Exception {
        OnboardingQuizRequestDto request = OnboardingQuizRequestDto.builder()
                .investmentHorizon("1Y")
                .riskTolerance("N10")
                .investmentAmount("5M")
                .applyToSettings(false)
                .build();
        OnboardingProfileResponseDto response = OnboardingProfileResponseDto.builder()
                .profile("BALANCED")
                .shortTermRatio(new BigDecimal("0.2"))
                .mediumTermRatio(new BigDecimal("0.4"))
                .longTermRatio(new BigDecimal("0.4"))
                .appliedToSettings(false)
                .build();
        when(onboardingService.submitProfile(eq(USER_ID), any(OnboardingQuizRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/onboarding/profile")
                        .with(SecurityMockMvcRequestPostProcessors.user(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile").value("BALANCED"))
                .andExpect(jsonPath("$.shortTermRatio").isNumber())
                .andExpect(jsonPath("$.mediumTermRatio").isNumber())
                .andExpect(jsonPath("$.longTermRatio").isNumber())
                .andExpect(jsonPath("$.appliedToSettings").exists());
    }
}
