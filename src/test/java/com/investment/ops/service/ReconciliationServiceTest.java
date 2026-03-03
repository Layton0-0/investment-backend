package com.investment.ops.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.domain.entity.StrategyPosition;
import com.investment.domain.repository.StrategyPositionRepository;
import com.investment.ops.dto.ReconciliationResultDto;
import com.investment.strategy.domain.StrategyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationService")
class ReconciliationServiceTest {

    private static final String USER_ID = "user-1";
    private static final String ACCOUNT_NO = "12345678-01";

    @Mock
    private AccountService accountService;
    @Mock
    private StrategyPositionRepository strategyPositionRepository;

    @InjectMocks
    private ReconciliationService reconciliationService;

    @Test
    @DisplayName("null userId 또는 accountNo면 빈 결과")
    void reconcile_nullInput_returnsEmpty() {
        ReconciliationResultDto result = reconciliationService.reconcile(null, ACCOUNT_NO);
        assertThat(result.getSummary().isHasDiscrepancy()).isFalse();
        assertThat(result.getMismatchItems()).isEmpty();
        assertThat(result.getOnlyInDb()).isEmpty();
        assertThat(result.getOnlyInBroker()).isEmpty();

        result = reconciliationService.reconcile(USER_ID, null);
        assertThat(result.getSummary().isHasDiscrepancy()).isFalse();
    }

    @Test
    @DisplayName("DB와 브로커 일치 시 불일치 없음")
    void reconcile_match_noDiscrepancy() {
        StrategyPosition pos = StrategyPosition.builder()
                .accountNo(ACCOUNT_NO)
                .symbol("005930")
                .market("KR")
                .strategyType(StrategyType.SHORT_TERM)
                .entryDt(LocalDate.now())
                .entryPrice(BigDecimal.valueOf(70000))
                .quantity(10)
                .atrMultiplier(BigDecimal.valueOf(2))
                .timeCutDays(5)
                .exitDt(null)
                .build();
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(ACCOUNT_NO))
                .thenReturn(List.of(pos));

        AccountPositionDto brokerPos = AccountPositionDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .quantity(10)
                .averagePrice(BigDecimal.valueOf(70000))
                .currentPrice(BigDecimal.valueOf(72000))
                .totalValue(BigDecimal.valueOf(720000))
                .profitLoss(BigDecimal.ZERO)
                .profitLossRate(BigDecimal.ZERO)
                .currency("KRW")
                .market("KR")
                .build();
        when(accountService.getBalanceAndPositionsWithUserId(eq(USER_ID), eq(ACCOUNT_NO)))
                .thenReturn(new BalanceAndPositionsDto(null, List.of(brokerPos)));

        ReconciliationResultDto result = reconciliationService.reconcile(USER_ID, ACCOUNT_NO);

        assertThat(result.getSummary().isHasDiscrepancy()).isFalse();
        assertThat(result.getMismatchItems()).isEmpty();
        assertThat(result.getOnlyInDb()).isEmpty();
        assertThat(result.getOnlyInBroker()).isEmpty();
    }

    @Test
    @DisplayName("DB에만 있으면 onlyInDb")
    void reconcile_onlyInDb() {
        StrategyPosition pos = StrategyPosition.builder()
                .accountNo(ACCOUNT_NO)
                .symbol("000660")
                .market("KR")
                .strategyType(StrategyType.SHORT_TERM)
                .entryDt(LocalDate.now())
                .entryPrice(BigDecimal.valueOf(100000))
                .quantity(5)
                .atrMultiplier(BigDecimal.valueOf(2))
                .timeCutDays(5)
                .exitDt(null)
                .build();
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(ACCOUNT_NO))
                .thenReturn(List.of(pos));
        when(accountService.getBalanceAndPositionsWithUserId(eq(USER_ID), eq(ACCOUNT_NO)))
                .thenReturn(new BalanceAndPositionsDto(null, List.of()));

        ReconciliationResultDto result = reconciliationService.reconcile(USER_ID, ACCOUNT_NO);

        assertThat(result.getSummary().isHasDiscrepancy()).isTrue();
        assertThat(result.getOnlyInDb()).hasSize(1);
        assertThat(result.getOnlyInDb().get(0).getSymbol()).isEqualTo("000660");
        assertThat(result.getOnlyInDb().get(0).getQuantity()).isEqualTo(5);
        assertThat(result.getOnlyInBroker()).isEmpty();
        assertThat(result.getMismatchItems()).isEmpty();
    }

    @Test
    @DisplayName("브로커에만 있으면 onlyInBroker")
    void reconcile_onlyInBroker() {
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(ACCOUNT_NO))
                .thenReturn(List.of());

        AccountPositionDto brokerPos = AccountPositionDto.builder()
                .symbol("035720")
                .name("카카오")
                .quantity(3)
                .averagePrice(BigDecimal.valueOf(50000))
                .currentPrice(BigDecimal.valueOf(52000))
                .totalValue(BigDecimal.valueOf(156000))
                .profitLoss(BigDecimal.ZERO)
                .profitLossRate(BigDecimal.ZERO)
                .currency("KRW")
                .market("KR")
                .build();
        when(accountService.getBalanceAndPositionsWithUserId(eq(USER_ID), eq(ACCOUNT_NO)))
                .thenReturn(new BalanceAndPositionsDto(null, List.of(brokerPos)));

        ReconciliationResultDto result = reconciliationService.reconcile(USER_ID, ACCOUNT_NO);

        assertThat(result.getSummary().isHasDiscrepancy()).isTrue();
        assertThat(result.getOnlyInBroker()).hasSize(1);
        assertThat(result.getOnlyInBroker().get(0).getSymbol()).isEqualTo("035720");
        assertThat(result.getOnlyInBroker().get(0).getQuantity()).isEqualTo(3);
        assertThat(result.getOnlyInDb()).isEmpty();
        assertThat(result.getMismatchItems()).isEmpty();
    }

    @Test
    @DisplayName("수량 불일치 시 mismatchItems")
    void reconcile_quantityMismatch() {
        StrategyPosition pos = StrategyPosition.builder()
                .accountNo(ACCOUNT_NO)
                .symbol("005930")
                .market("KR")
                .strategyType(StrategyType.SHORT_TERM)
                .entryDt(LocalDate.now())
                .entryPrice(BigDecimal.valueOf(70000))
                .quantity(10)
                .atrMultiplier(BigDecimal.valueOf(2))
                .timeCutDays(5)
                .exitDt(null)
                .build();
        when(strategyPositionRepository.findByAccountNoAndExitDtIsNullOrderByEntryDtAsc(ACCOUNT_NO))
                .thenReturn(List.of(pos));

        AccountPositionDto brokerPos = AccountPositionDto.builder()
                .symbol("005930")
                .name("삼성전자")
                .quantity(8)
                .averagePrice(BigDecimal.valueOf(70000))
                .currentPrice(BigDecimal.valueOf(72000))
                .totalValue(BigDecimal.valueOf(576000))
                .profitLoss(BigDecimal.ZERO)
                .profitLossRate(BigDecimal.ZERO)
                .currency("KRW")
                .market("KR")
                .build();
        when(accountService.getBalanceAndPositionsWithUserId(eq(USER_ID), eq(ACCOUNT_NO)))
                .thenReturn(new BalanceAndPositionsDto(null, List.of(brokerPos)));

        ReconciliationResultDto result = reconciliationService.reconcile(USER_ID, ACCOUNT_NO);

        assertThat(result.getSummary().isHasDiscrepancy()).isTrue();
        assertThat(result.getMismatchItems()).hasSize(1);
        assertThat(result.getMismatchItems().get(0).getSymbol()).isEqualTo("005930");
        assertThat(result.getMismatchItems().get(0).getDbQuantity()).isEqualTo(10);
        assertThat(result.getMismatchItems().get(0).getBrokerQuantity()).isEqualTo(8);
        assertThat(result.getOnlyInDb()).isEmpty();
        assertThat(result.getOnlyInBroker()).isEmpty();
    }
}
