package com.investment.auth.service;

import com.investment.auth.dto.AccountVerifyRequestDto;
import com.investment.auth.dto.AccountVerifyResponseDto;
import com.investment.marketdata.service.KoreaInvestmentTokenClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountVerificationService")
class AccountVerificationServiceTest {

    @Mock
    private KoreaInvestmentTokenClient tokenClient;

    @InjectMocks
    private AccountVerificationService accountVerificationService;

    private static AccountVerifyRequestDto request() {
        AccountVerifyRequestDto dto = new AccountVerifyRequestDto();
        dto.setAppKey("key");
        dto.setAppSecret("secret");
        dto.setServerType("1");
        dto.setAccountNo("12345678-12");
        return dto;
    }

    @Test
    @DisplayName("verifyAccount 토큰 발급 성공 시 success true 반환")
    void verifyAccount_tokenSuccess_returnsSuccess() {
        when(tokenClient.issueAccessToken(anyString(), anyString(), anyString()))
                .thenReturn(Mono.just("access-token-123"));

        AccountVerifyResponseDto result = accountVerificationService.verifyAccount(request());

        assertTrue(result.isSuccess());
        assertEquals("계좌 인증이 완료되었습니다.", result.getMessage());
        assertEquals("access-token-123", result.getAccessToken());
    }

    @Test
    @DisplayName("verifyAccount 토큰 null 시 success false 반환")
    void verifyAccount_tokenNull_returnsFailure() {
        when(tokenClient.issueAccessToken(anyString(), anyString(), anyString()))
                .thenReturn(Mono.empty());

        AccountVerifyResponseDto result = accountVerificationService.verifyAccount(request());

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
        assertNull(result.getAccessToken());
    }

    @Test
    @DisplayName("verifyAccount 예외 발생 시 success false·메시지 반환")
    void verifyAccount_exception_returnsFailure() {
        when(tokenClient.issueAccessToken(anyString(), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Connection refused")));

        AccountVerifyResponseDto result = accountVerificationService.verifyAccount(request());

        assertFalse(result.isSuccess());
        assertNotNull(result.getMessage());
    }
}
