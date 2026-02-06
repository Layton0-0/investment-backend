package com.investment.core.engine.risk;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.order.dto.OrderRequestDto;
import com.investment.risk.service.PortfolioPeakService;
import com.investment.risk.service.TradingHaltService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreTradeComplianceEngine")
class PreTradeComplianceEngineTest {

    @Mock
    private TradingHaltService tradingHaltService;
    @Mock
    private PortfolioPeakService portfolioPeakService;
    @Mock
    private AccountService accountService;

    private PreTradeComplianceEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PreTradeComplianceEngine(tradingHaltService, portfolioPeakService, accountService);
    }

    @Test
    @DisplayName("Kill Switch 활성화 시 거부")
    void preTradeCheck_whenHalt_rejects() {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(true);

        OrderRequestDto request = OrderRequestDto.builder()
                .accountNo("123")
                .symbol("005930")
                .orderType(OrderRequestDto.OrderType.BUY)
                .quantity(10)
                .price(BigDecimal.valueOf(70000))
                .build();

        ComplianceResult result = engine.preTradeCheck(request, "user1");

        assertFalse(result.isApproved());
        assertTrue(result.getReason().contains("Kill Switch"));
    }

    @Test
    @DisplayName("잔고 조회 실패 시 승인(스킵)")
    void preTradeCheck_whenBalanceNull_approves() {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(false);
        when(accountService.getBalanceAndPositionsWithUserId(eq("user1"), eq("123"))).thenReturn(null);

        OrderRequestDto request = OrderRequestDto.builder()
                .accountNo("123")
                .symbol("005930")
                .orderType(OrderRequestDto.OrderType.BUY)
                .quantity(10)
                .price(BigDecimal.valueOf(70000))
                .build();

        ComplianceResult result = engine.preTradeCheck(request, "user1");

        assertTrue(result.isApproved());
    }

    @Test
    @DisplayName("정상 잔고·MDD 이내·비중 이내 시 승인")
    void preTradeCheck_whenOk_approves() {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(false);
        AccountBalanceDto balance = AccountBalanceDto.builder()
                .accountNo("123")
                .totalBalance(BigDecimal.valueOf(10_000_000))
                .totalAssetValue(BigDecimal.valueOf(10_000_000))
                .availableBalance(BigDecimal.valueOf(5_000_000))
                .investedAmount(BigDecimal.valueOf(5_000_000))
                .currency("KRW")
                .build();
        when(accountService.getBalanceAndPositionsWithUserId(eq("user1"), eq("123")))
                .thenReturn(new BalanceAndPositionsDto(balance, List.of()));
        when(portfolioPeakService.getOrUpdatePeakAndComputeMdd(eq("123"), any(), any()))
                .thenReturn(BigDecimal.valueOf(0.05));

        OrderRequestDto request = OrderRequestDto.builder()
                .accountNo("123")
                .symbol("005930")
                .orderType(OrderRequestDto.OrderType.BUY)
                .quantity(10)
                .price(BigDecimal.valueOf(70000)) // 70만원 → 비중 7%
                .build();

        ComplianceResult result = engine.preTradeCheck(request, "user1");

        assertTrue(result.isApproved());
    }
}
