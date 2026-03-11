package com.investment.auth.controller;

import com.investment.account.service.AccountService;
import com.investment.auth.dto.*;
import com.investment.auth.service.AccountVerificationService;
import com.investment.auth.service.AuthService;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AuthService authService;

        @MockBean
        private AccountVerificationService accountVerificationService;
        @MockBean
        private AccountService accountService;
        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        @MockBean
        private RateLimitFilter rateLimitFilter;
        @MockBean
        private SecurityHeadersConfig securityHeadersConfig;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        @DisplayName("POST /api/v1/auth/signup 회원가입 성공 시 201")
        void signup_returnsCreated() throws Exception {
                SignupRequestDto request = new SignupRequestDto();
                request.setUsername("testuser");
                request.setPassword("MyP@ssw0rd1");
                request.setBrokerType("KOREA_INVESTMENT");
                request.setAppKey("appKey");
                request.setAppSecret("appSecret");
                request.setServerType("1");
                request.setAccountNo("12345678-12");

                AuthResponseDto response = AuthResponseDto.builder()
                                .token("jwt-token")
                                .userId("user-id")
                                .username("testuser")
                                .message("회원가입 완료")
                                .build();
                when(authService.signup(any(SignupRequestDto.class))).thenReturn(response);

                mockMvc.perform(post("/api/v1/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.token").value("jwt-token"))
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/login 로그인 성공 시 200")
        void login_returnsOk() throws Exception {
                LoginRequestDto request = new LoginRequestDto();
                request.setUsername("testuser");
                request.setPassword("MyP@ssw0rd1");

                AuthResponseDto response = AuthResponseDto.builder()
                                .token("jwt-token")
                                .userId("user-id")
                                .username("testuser")
                                .message("로그인 성공")
                                .build();
                when(authService.login(any(LoginRequestDto.class), anyString())).thenReturn(response);

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").value("jwt-token"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/verify-account 계좌인증 성공 시 200")
        void verifyAccount_returnsOk() throws Exception {
                AccountVerifyRequestDto request = new AccountVerifyRequestDto();
                request.setAppKey("key");
                request.setAppSecret("secret");
                request.setServerType("1");
                request.setAccountNo("12345678-12");

                AccountVerifyResponseDto response = AccountVerifyResponseDto.builder()
                                .success(true)
                                .message("계좌 인증 성공")
                                .build();
                when(accountVerificationService.verifyAccount(any(AccountVerifyRequestDto.class))).thenReturn(response);

                mockMvc.perform(post("/api/v1/auth/verify-account")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @org.junit.jupiter.api.Disabled("addFilters=false 시 SecurityContext 미전달로 authentication null → 통합/전체 컨텍스트에서 검증")
        @DisplayName("GET /api/v1/auth/mypage 인증 시 마이페이지 조회 200")
        void getMyPage_returnsOk() throws Exception {
                MyPageResponseDto response = MyPageResponseDto.builder()
                                .username("testuser")
                                .brokerType("KOREA_INVESTMENT")
                                .serverType("1")
                                .build();
                when(authService.getMyPage(anyString())).thenReturn(response);

                mockMvc.perform(get("/api/v1/auth/mypage")
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                                .user("user-id")))
                                .andExpect(status().isOk());
                verify(authService).getMyPage(anyString());
        }

        @Test
        @org.junit.jupiter.api.Disabled("addFilters=false 시 SecurityContext 미전달로 authentication null → 통합/전체 컨텍스트에서 검증")
        @DisplayName("GET /api/v1/auth/tokens 인증 시 토큰 조회 200")
        void getTokens_returnsOkWhenAuthenticated() throws Exception {
                AuthTokensResponseDto response = AuthTokensResponseDto.builder()
                                .accessToken("access-token-value")
                                .websocketToken("ws-token-value")
                                .build();
                when(authService.getTokens(anyString(), any())).thenReturn(response);

                mockMvc.perform(get("/api/v1/auth/tokens")
                                .param("serverType", "1")
                                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                                .user("user-id")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-token-value"))
                                .andExpect(jsonPath("$.websocketToken").value("ws-token-value"));
                verify(authService).getTokens(eq("user-id"), eq("1"));
        }

        @Test
        @DisplayName("POST /api/v1/auth/logout 로그아웃 시 200")
        void logout_returnsOk() throws Exception {
                mockMvc.perform(post("/api/v1/auth/logout"))
                                .andExpect(status().isOk());
        }
}
