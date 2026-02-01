package com.investment.account.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.client.KoreaInvestmentAccountClient;
import com.investment.common.security.EncryptionUtil;
import com.investment.domain.repository.PortfolioRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private KoreaInvestmentAccountClient accountClient;

    @Mock
    private KoreaInvestmentTokenService tokenService;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("getAccountBalance 인증 없을 때 DB 폴백으로 잔고 반환")
    void getAccountBalance_noAuth_returnsDbFallback() {
        String accountNo = "12345678-12";
        BigDecimal portfolioValue = BigDecimal.valueOf(1_000_000);

        SecurityContextHolder.clearContext();
        when(portfolioRepository.getTotalPortfolioValue(accountNo)).thenReturn(portfolioValue);

        AccountBalanceDto result = accountService.getAccountBalance(accountNo);

        assertNotNull(result);
        assertEquals(accountNo, result.getAccountNo());
        assertEquals(portfolioValue, result.getTotalBalance());
        assertEquals(portfolioValue, result.getAvailableBalance());
        assertEquals("KRW", result.getCurrency());
        verify(portfolioRepository).getTotalPortfolioValue(accountNo);
    }

    @Test
    @DisplayName("getAccountBalance DB 포트폴리오 값 null 시 0으로 반환")
    void getAccountBalance_dbNull_returnsZeroBalance() {
        String accountNo = "12345678-12";
        SecurityContextHolder.clearContext();
        when(portfolioRepository.getTotalPortfolioValue(accountNo)).thenReturn(null);

        AccountBalanceDto result = accountService.getAccountBalance(accountNo);

        assertNotNull(result);
        assertEquals(accountNo, result.getAccountNo());
        assertEquals(BigDecimal.ZERO, result.getTotalBalance());
        assertEquals(BigDecimal.ZERO, result.getAvailableBalance());
    }

    @Test
    @DisplayName("getBalanceAndPositions API 성공 시 잔고와 보유종목 한 번에 반환")
    void getBalanceAndPositions_apiSuccess_returnsBoth() {
        String accountNo = "12345678-12";
        String userId = "user-1";
        AccountBalanceDto balanceDto = AccountBalanceDto.builder()
                .accountNo(accountNo)
                .totalBalance(BigDecimal.valueOf(10_000_000))
                .currency("KRW")
                .build();
        List<AccountPositionDto> positions = List.of();
        KoreaInvestmentAccountClient.BalanceAndPositionsResult clientResult =
                new KoreaInvestmentAccountClient.BalanceAndPositionsResult(balanceDto, positions);

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(accountClient.inquireBalance(eq(userId), eq(accountNo))).thenReturn(clientResult);

        BalanceAndPositionsDto result = accountService.getBalanceAndPositions(accountNo);

        assertNotNull(result);
        assertEquals(balanceDto, result.getBalance());
        assertEquals(positions, result.getPositions());
        verify(accountClient, times(1)).inquireBalance(userId, accountNo);
        SecurityContextHolder.clearContext();
    }
}
