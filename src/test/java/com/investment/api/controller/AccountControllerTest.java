package com.investment.api.controller;

import com.investment.account.dto.*;
import com.investment.account.service.AccountService;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.exception.GlobalExceptionHandler;
import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import com.investment.config.SecurityHeadersConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AccountController")
class AccountControllerTest {

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

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/balance 잔고 조회 성공")
    void getBalance_returnsOk() throws Exception {
        AccountBalanceDto dto = AccountBalanceDto.builder()
                .accountNo("12345678")
                .totalBalance(BigDecimal.valueOf(1_000_000))
                .availableBalance(BigDecimal.valueOf(950_000))
                .investedAmount(BigDecimal.valueOf(50_000))
                .currency("KRW")
                .build();
        when(accountService.getAccountBalance("12345678")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("12345678"))
                .andExpect(jsonPath("$.currency").value("KRW"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/positions 보유 종목 조회 성공")
    void getPositions_returnsOk() throws Exception {
        when(accountService.getPositions(anyString(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts/12345678/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/buyable-amount 매수가능 조회 성공")
    void getBuyableAmount_returnsOk() throws Exception {
        BuyableAmountDto dto = BuyableAmountDto.builder()
                .accountNo("12345678")
                .symbol("005930")
                .price(BigDecimal.valueOf(75000))
                .buyableAmount(BigDecimal.valueOf(900_000))
                .buyableQuantity(12)
                .currency("KRW")
                .build();
        when(accountService.getBuyableAmount(eq("12345678"), eq("005930"), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678/buyable-amount")
                        .param("symbol", "005930")
                        .param("price", "75000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("005930"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/sellable-quantity 매도가능 수량 조회 성공")
    void getSellableQuantity_returnsOk() throws Exception {
        SellableQuantityDto dto = SellableQuantityDto.builder()
                .accountNo("12345678")
                .symbol("005930")
                .sellableQuantity(100)
                .holdingQuantity(100)
                .averagePrice(BigDecimal.valueOf(70000))
                .currency("KRW")
                .build();
        when(accountService.getSellableQuantity("12345678", "005930")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678/sellable-quantity")
                        .param("symbol", "005930"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("005930"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/order-history 주문체결 조회 성공")
    void getOrderHistory_returnsOk() throws Exception {
        when(accountService.getOrderHistory(eq("12345678"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts/12345678/order-history")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/cancelable-orders 정정취소가능주문 조회 성공")
    void getCancelableOrders_returnsOk() throws Exception {
        when(accountService.getCancelableOrders("12345678")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/accounts/12345678/cancelable-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/assets 자산현황 조회 성공")
    void getAccountAssets_returnsOk() throws Exception {
        AccountAssetDto dto = AccountAssetDto.builder()
                .accountNo("12345678")
                .totalAssetValue(BigDecimal.valueOf(10_000_000))
                .deposit(BigDecimal.ZERO)
                .stockValue(BigDecimal.ZERO)
                .totalProfitLoss(BigDecimal.ZERO)
                .totalProfitLossRate(BigDecimal.ZERO)
                .orderableCash(BigDecimal.ZERO)
                .currency("KRW")
                .build();
        when(accountService.getAccountAssets("12345678")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("12345678"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/assets 계좌 없을 때 404")
    void getAccountAssets_accountNotFound_returns404() throws Exception {
        when(accountService.getAccountAssets("99999999"))
                .thenThrow(new DomainException(ErrorCode.ACCOUNT_NOT_FOUND, "한국투자증권 API 키를 찾을 수 없습니다"));

        mockMvc.perform(get("/api/v1/accounts/99999999/assets"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/profit-loss 기간별손익 조회 성공")
    void getPeriodProfitLoss_returnsOk() throws Exception {
        ProfitLossDto dto = ProfitLossDto.builder()
                .accountNo("12345678")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .totalProfitLoss(BigDecimal.valueOf(500_000))
                .totalProfitLossRate(BigDecimal.ZERO)
                .realizedProfitLoss(BigDecimal.ZERO)
                .unrealizedProfitLoss(BigDecimal.ZERO)
                .currency("KRW")
                .build();
        when(accountService.getPeriodProfitLoss(eq("12345678"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678/profit-loss")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("12345678"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/balance-rlz-pl 실현손익 조회 성공")
    void getBalanceRealizedProfitLoss_returnsOk() throws Exception {
        BalanceRealizedProfitLossDto dto = BalanceRealizedProfitLossDto.builder()
                .accountNo("12345678-01")
                .totalRealizedProfitLoss(BigDecimal.valueOf(100_000))
                .currency("KRW")
                .details(List.of())
                .build();
        when(accountService.getBalanceRealizedProfitLoss("12345678-01")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678-01/balance-rlz-pl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("12345678-01"))
                .andExpect(jsonPath("$.totalRealizedProfitLoss").value(100_000))
                .andExpect(jsonPath("$.currency").value("KRW"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNo}/profit-loss-status 기간별매매손익현황 조회 성공")
    void getPeriodProfitLossStatus_returnsOk() throws Exception {
        PeriodProfitLossStatusDto dto = PeriodProfitLossStatusDto.builder()
                .accountNo("12345678-01")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .totalRealizedProfitLoss(BigDecimal.valueOf(50_000))
                .currency("KRW")
                .items(List.of())
                .build();
        when(accountService.getPeriodProfitLossStatus(eq("12345678-01"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(dto);

        mockMvc.perform(get("/api/v1/accounts/12345678-01/profit-loss-status")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNo").value("12345678-01"))
                .andExpect(jsonPath("$.totalRealizedProfitLoss").value(50_000));
    }
}
