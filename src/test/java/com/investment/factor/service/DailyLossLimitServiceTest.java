package com.investment.factor.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.config.RiskProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailyLossLimitService")
class DailyLossLimitServiceTest {

    @Mock
    private RiskProperties riskProperties;
    @Mock
    private TradingSettingRepository tradingSettingRepository;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private DailyLossLimitService dailyLossLimitService;

    private static final String ACCOUNT_NO = "1234567890";
    private static final String USER_ID = "user1";

    @BeforeEach
    void setUp() {
        lenient().when(riskProperties.getDailyLossLimitPct()).thenReturn(new BigDecimal("5"));
        TradingSetting setting = TradingSetting.builder()
                .accountNo(ACCOUNT_NO)
                .userId(USER_ID)
                .maxInvestmentAmount(BigDecimal.valueOf(10_000_000))
                .minInvestmentAmount(BigDecimal.valueOf(10_000))
                .defaultCurrency("KRW")
                .autoTradingEnabled(true)
                .build();
        lenient().when(tradingSettingRepository.findByAccountNo(ACCOUNT_NO)).thenReturn(Optional.of(setting));
    }

    @Test
    @DisplayName("시초 평가액 미기록 시 isNewBuyAllowed true")
    void isNewBuyAllowed_noOpeningRecord_allowed() {
        when(accountService.getBalanceAndPositionsWithUserId(USER_ID, ACCOUNT_NO))
                .thenReturn(balanceDto(new BigDecimal("10000000")));

        boolean allowed = dailyLossLimitService.isNewBuyAllowed(ACCOUNT_NO);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("시초 기록 후 손실 한도 미초과 시 허용")
    void isNewBuyAllowed_withinLimit_allowed() {
        dailyLossLimitService.recordOpeningBalanceIfAbsent(ACCOUNT_NO, new BigDecimal("10000000"));
        when(accountService.getBalanceAndPositionsWithUserId(USER_ID, ACCOUNT_NO))
                .thenReturn(balanceDto(new BigDecimal("9700000")));

        boolean allowed = dailyLossLimitService.isNewBuyAllowed(ACCOUNT_NO);

        assertThat(allowed).isTrue();
    }

    @Test
    @DisplayName("일일 손실 한도 초과 시 신규 매수 불가")
    void isNewBuyAllowed_exceedsLimit_notAllowed() {
        dailyLossLimitService.recordOpeningBalanceIfAbsent(ACCOUNT_NO, new BigDecimal("10000000"));
        when(accountService.getBalanceAndPositionsWithUserId(USER_ID, ACCOUNT_NO))
                .thenReturn(balanceDto(new BigDecimal("9400000")));

        boolean allowed = dailyLossLimitService.isNewBuyAllowed(ACCOUNT_NO);

        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("getOpeningBalance - 기록된 시초 평가액 반환")
    void getOpeningBalance_returnsRecorded() {
        dailyLossLimitService.recordOpeningBalanceIfAbsent(ACCOUNT_NO, new BigDecimal("10000000"));

        BigDecimal opening = dailyLossLimitService.getOpeningBalance(ACCOUNT_NO, LocalDate.now());

        assertThat(opening).isEqualByComparingTo(new BigDecimal("10000000"));
    }

    @Test
    @DisplayName("getOpeningBalance - 미기록 시 null")
    void getOpeningBalance_notRecorded_returnsNull() {
        BigDecimal opening = dailyLossLimitService.getOpeningBalance(ACCOUNT_NO, LocalDate.now());

        assertThat(opening).isNull();
    }

    @Test
    @DisplayName("일일 손실 한도 0 이하 설정 시 항상 허용")
    void isNewBuyAllowed_limitZeroOrNull_allowed() {
        when(riskProperties.getDailyLossLimitPct()).thenReturn(BigDecimal.ZERO);
        dailyLossLimitService.recordOpeningBalanceIfAbsent(ACCOUNT_NO, new BigDecimal("10000000"));
        // limitPct 0 이하이므로 getCurrentPortfolioValue 호출 없이 즉시 허용

        boolean allowed = dailyLossLimitService.isNewBuyAllowed(ACCOUNT_NO);

        assertThat(allowed).isTrue();
    }

    private static BalanceAndPositionsDto balanceDto(BigDecimal totalAssetValue) {
        AccountBalanceDto balance = AccountBalanceDto.builder()
                .accountNo(ACCOUNT_NO)
                .totalBalance(totalAssetValue)
                .availableBalance(totalAssetValue)
                .investedAmount(BigDecimal.ZERO)
                .currency("KRW")
                .totalAssetValue(totalAssetValue)
                .build();
        return BalanceAndPositionsDto.builder()
                .balance(balance)
                .positions(Collections.emptyList())
                .build();
    }
}
