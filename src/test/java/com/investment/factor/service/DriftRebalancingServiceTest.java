package com.investment.factor.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.core.engine.portfolio.Rebalancer;
import com.investment.marketdata.service.RealtimeMarketDataService;
import com.investment.order.service.OrderService;
import com.investment.tradingportfolio.service.RebalanceSuggestionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DriftRebalancingService")
class DriftRebalancingServiceTest {

    private static final String USER_ID = "user1";
    private static final String ACCOUNT_NO = "1234567890";

    @Mock
    private AccountService accountService;
    @Mock
    private RebalanceSuggestionsService rebalanceSuggestionsService;
    @Mock
    private Rebalancer rebalancer;
    @Mock
    private OrderService orderService;
    @Mock
    private RealtimeMarketDataService realtimeMarketDataService;

    @InjectMocks
    private DriftRebalancingService driftRebalancingService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(driftRebalancingService, "driftTolerancePct", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(driftRebalancingService, "driftRebalanceEnabled", true);
    }

    @Test
    @DisplayName("KR 시장이면 getRebalanceListIfDriftExceeded 빈 목록 반환")
    void getRebalanceListIfDriftExceeded_kr_returnsEmpty() {
        List<Rebalancer.RebalanceItem> result = driftRebalancingService.getRebalanceListIfDriftExceeded(
                USER_ID, ACCOUNT_NO, "KR");

        assertThat(result).isEmpty();
        verify(accountService, never()).getBalanceAndPositionsWithUserId(any(), any());
    }

    @Test
    @DisplayName("드리프트 5% 초과 시 getRebalanceListIfDriftExceeded 리밸런스 목록 반환")
    void getRebalanceListIfDriftExceeded_driftExceeds5Percent_returnsList() {
        // 현재: SPY 70%, QQQ 30% / 목표: SPY 50%, QQQ 50% → max drift 20%
        BalanceAndPositionsDto balanceAndPositions = balanceWithUsPositions(
                new BigDecimal("10000"),
                List.of(
                        position("SPY", new BigDecimal("7000")),
                        position("QQQ", new BigDecimal("3000"))
                ));
        when(accountService.getBalanceAndPositionsWithUserId(USER_ID, ACCOUNT_NO))
                .thenReturn(balanceAndPositions);
        when(rebalanceSuggestionsService.getTargetWeightsForUs())
                .thenReturn(Map.of("SPY", new BigDecimal("0.5"), "QQQ", new BigDecimal("0.5")));
        List<Rebalancer.RebalanceItem> expectedItems = List.of(
                new Rebalancer.RebalanceItem("SPY", "SELL", null, new BigDecimal("2000")),
                new Rebalancer.RebalanceItem("QQQ", "BUY", null, new BigDecimal("2000"))
        );
        when(rebalancer.computeRebalanceList(any(), eq(ACCOUNT_NO), eq("US"), any(), any(), eq(USER_ID)))
                .thenReturn(expectedItems);

        List<Rebalancer.RebalanceItem> result = driftRebalancingService.getRebalanceListIfDriftExceeded(
                USER_ID, ACCOUNT_NO, "US");

        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedItems);
        verify(rebalancer).computeRebalanceList(any(), eq(ACCOUNT_NO), eq("US"), any(), any(), eq(USER_ID));
    }

    @Test
    @DisplayName("드리프트 5% 미만이면 getRebalanceListIfDriftExceeded 빈 목록 반환")
    void getRebalanceListIfDriftExceeded_driftUnder5Percent_returnsEmpty() {
        // 현재: SPY 52%, QQQ 48% / 목표: 50%, 50% → max drift 2%
        BalanceAndPositionsDto balanceAndPositions = balanceWithUsPositions(
                new BigDecimal("10000"),
                List.of(
                        position("SPY", new BigDecimal("5200")),
                        position("QQQ", new BigDecimal("4800"))
                ));
        when(accountService.getBalanceAndPositionsWithUserId(USER_ID, ACCOUNT_NO))
                .thenReturn(balanceAndPositions);
        when(rebalanceSuggestionsService.getTargetWeightsForUs())
                .thenReturn(Map.of("SPY", new BigDecimal("0.5"), "QQQ", new BigDecimal("0.5")));

        List<Rebalancer.RebalanceItem> result = driftRebalancingService.getRebalanceListIfDriftExceeded(
                USER_ID, ACCOUNT_NO, "US");

        assertThat(result).isEmpty();
        verify(rebalancer, never()).computeRebalanceList(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("목표 비중이 비어 있으면 빈 목록 반환")
    void getRebalanceListIfDriftExceeded_emptyTargetWeights_returnsEmpty() {
        BalanceAndPositionsDto balanceAndPositions = balanceWithUsPositions(
                new BigDecimal("10000"),
                List.of(position("SPY", new BigDecimal("7000"))));
        when(accountService.getBalanceAndPositionsWithUserId(USER_ID, ACCOUNT_NO))
                .thenReturn(balanceAndPositions);
        when(rebalanceSuggestionsService.getTargetWeightsForUs()).thenReturn(Map.of());

        List<Rebalancer.RebalanceItem> result = driftRebalancingService.getRebalanceListIfDriftExceeded(
                USER_ID, ACCOUNT_NO, "US");

        assertThat(result).isEmpty();
        verify(rebalancer, never()).computeRebalanceList(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("checkAndExecuteDriftRebalance KR 시장이면 주문 미호출")
    void checkAndExecuteDriftRebalance_kr_doesNotExecute() {
        driftRebalancingService.checkAndExecuteDriftRebalance(USER_ID, ACCOUNT_NO, "KR");

        verify(accountService, never()).getBalanceAndPositionsWithUserId(any(), any());
        verify(orderService, never()).executeOrderForPipeline(any(), any());
    }

    private static BalanceAndPositionsDto balanceWithUsPositions(BigDecimal totalValue,
            List<AccountPositionDto> usPositions) {
        AccountBalanceDto balance = AccountBalanceDto.builder()
                .accountNo(ACCOUNT_NO)
                .totalBalance(totalValue)
                .totalAssetValue(totalValue)
                .availableBalance(totalValue)
                .investedAmount(BigDecimal.ZERO)
                .currency("USD")
                .build();
        return BalanceAndPositionsDto.builder()
                .balance(balance)
                .positions(usPositions)
                .build();
    }

    private static AccountPositionDto position(String symbol, BigDecimal totalValue) {
        return AccountPositionDto.builder()
                .symbol(symbol)
                .name(symbol)
                .quantity(10)
                .averagePrice(BigDecimal.ONE)
                .currentPrice(totalValue.divide(BigDecimal.TEN, 2, java.math.RoundingMode.HALF_UP))
                .totalValue(totalValue)
                .profitLoss(BigDecimal.ZERO)
                .profitLossRate(BigDecimal.ZERO)
                .currency("USD")
                .market("US")
                .build();
    }
}
