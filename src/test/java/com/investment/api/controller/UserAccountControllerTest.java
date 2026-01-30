package com.investment.api.controller;

import com.investment.account.dto.AccountListResponseDto;
import com.investment.account.dto.MainAccountResponseDto;
import com.investment.account.service.AccountService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserAccountController")
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean
    private RateLimitFilter rateLimitFilter;
    @MockBean
    private SecurityHeadersConfig securityHeadersConfig;

    private static Authentication auth(String name) {
        return new UsernamePasswordAuthenticationToken(name, null, java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("GET /api/v1/user/accounts 계좌 목록 조회 성공")
    void getUserAccounts_returnsOk() throws Exception {
        AccountListResponseDto response = AccountListResponseDto.builder()
                .accounts(java.util.Collections.emptyList())
                .totalCount(0)
                .build();
        when(accountService.getUserAccounts("user1", null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/user/accounts")
                        .principal(auth("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));
        verify(accountService).getUserAccounts("user1", null);
    }

    @Test
    @DisplayName("GET /api/v1/user/accounts?serverType=1 서버타입 필터로 계좌 목록 조회")
    void getUserAccounts_withServerType_callsServiceWithServerType() throws Exception {
        AccountListResponseDto response = AccountListResponseDto.builder()
                .accounts(java.util.Collections.emptyList())
                .totalCount(0)
                .build();
        when(accountService.getUserAccounts("user1", "1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/user/accounts").param("serverType", "1")
                        .principal(auth("user1")))
                .andExpect(status().isOk());
        verify(accountService).getUserAccounts("user1", "1");
    }

    @Test
    @DisplayName("GET /api/v1/user/accounts/main 메인 계좌 조회 성공")
    void getMainAccount_returnsOk() throws Exception {
        MainAccountResponseDto response = MainAccountResponseDto.builder()
                .accountId("acc-1")
                .accountNo("12345678")
                .accountNoMasked("1234****")
                .brokerType("KIS")
                .serverType("1")
                .build();
        when(accountService.getMainAccount("user1", null)).thenReturn(response);

        mockMvc.perform(get("/api/v1/user/accounts/main")
                        .principal(auth("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc-1"));
    }

    @Test
    @DisplayName("GET /api/v1/user/accounts/main 계좌 없을 때 404")
    void getMainAccount_notFound_returns404() throws Exception {
        when(accountService.getMainAccount("user1", null))
                .thenThrow(new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "계좌 없음"));

        mockMvc.perform(get("/api/v1/user/accounts/main")
                        .principal(auth("user1")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/user/accounts/{accountId} 특정 계좌 조회 성공")
    void getAccount_returnsOk() throws Exception {
        MainAccountResponseDto response = MainAccountResponseDto.builder()
                .accountId("acc-1")
                .accountNo("12345678")
                .build();
        when(accountService.getAccountByAccountId("user1", "acc-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/user/accounts/acc-1")
                        .principal(auth("user1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acc-1"));
    }

    @Test
    @DisplayName("GET /api/v1/user/accounts/{accountId} 계좌 없을 때 404")
    void getAccount_notFound_returns404() throws Exception {
        when(accountService.getAccountByAccountId("user1", "acc-999"))
                .thenThrow(new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "계좌 없음"));

        mockMvc.perform(get("/api/v1/user/accounts/acc-999")
                        .principal(auth("user1")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/v1/user/accounts/{accountId}/main 메인 계좌 변경 성공")
    void setMainAccount_returnsOk() throws Exception {
        doNothing().when(accountService).setMainAccount("user1", "acc-1");

        mockMvc.perform(put("/api/v1/user/accounts/acc-1/main")
                        .principal(auth("user1")))
                .andExpect(status().isOk());
        verify(accountService).setMainAccount("user1", "acc-1");
    }

    @Test
    @DisplayName("PUT /api/v1/user/accounts/{accountId}/main 계좌 없을 때 404")
    void setMainAccount_notFound_returns404() throws Exception {
        doThrow(new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "계좌 없음"))
                .when(accountService).setMainAccount("user1", "acc-999");

        mockMvc.perform(put("/api/v1/user/accounts/acc-999/main")
                        .principal(auth("user1")))
                .andExpect(status().isNotFound());
    }
}
