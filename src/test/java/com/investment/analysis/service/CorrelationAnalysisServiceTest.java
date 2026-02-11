package com.investment.analysis.service;

import com.investment.account.dto.AccountBalanceDto;
import com.investment.account.dto.AccountPositionDto;
import com.investment.account.dto.BalanceAndPositionsDto;
import com.investment.account.service.AccountService;
import com.investment.analysis.dto.CorrelationAnalysisResponseDto;
import com.investment.domain.entity.DailyStock;
import com.investment.domain.repository.DailyStockRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationAnalysisService")
class CorrelationAnalysisServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;
    @Mock
    private AccountService accountService;

    @InjectMocks
    private CorrelationAnalysisService correlationAnalysisService;

    @Test
    @DisplayName("종목 2개 미만이면 빈 행렬 반환")
    void getCorrelationBySymbols_lessThanTwoSymbols_returnsEmptyMatrix() {
        CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationBySymbols(
                List.of("AAPL"), "US", null, null);

        assertThat(response.getSymbols()).isEmpty();
        assertThat(response.getMatrix()).isEmpty();
        assertThat(response.getMarket()).isEqualTo("US");
    }

    @Test
    @DisplayName("symbols 비어 있으면 빈 행렬 반환")
    void getCorrelationBySymbols_emptySymbols_returnsEmptyMatrix() {
        CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationBySymbols(
                List.of(), "US", null, null);

        assertThat(response.getSymbols()).isEmpty();
        assertThat(response.getMatrix()).isEmpty();
    }

    @Test
    @DisplayName("일봉 데이터 충분하면 상관계수 행렬 반환")
    void getCorrelationBySymbols_sufficientData_returnsMatrix() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 2, 10);
        List<String> symbols = List.of("AAPL", "MSFT");
        List<DailyStock> rows = List.of();
        for (int d = 0; d < 25; d++) {
            LocalDate dt = from.plusDays(d);
            rows = new java.util.ArrayList<>(rows);
            rows.add(DailyStock.builder().basDt(dt).symbol("AAPL").market("US").closePrice(BigDecimal.valueOf(100 + d)).build());
            rows.add(DailyStock.builder().basDt(dt).symbol("MSFT").market("US").closePrice(BigDecimal.valueOf(200 + d * 2)).build());
        }
        when(dailyStockRepository.findByMarketAndSymbolInAndBasDtBetweenOrderByBasDtAsc(
                eq("US"), eq(symbols), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(rows);

        CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationBySymbols(
                symbols, "US", from, to);

        assertThat(response.getMarket()).isEqualTo("US");
        assertThat(response.getSymbols()).hasSize(2);
        assertThat(response.getMatrix()).hasSize(2);
        assertThat(response.getMatrix().get(0)).hasSize(2);
        assertThat(response.getMatrix().get(0).get(0)).isEqualTo(1.0);
        assertThat(response.getMatrix().get(1).get(1)).isEqualTo(1.0);
    }

    @Test
    @DisplayName("accountNo 기준 보유 0건이면 빈 행렬 반환")
    void getCorrelationByAccount_noPositions_returnsEmptyMatrix() {
        when(accountService.getBalanceAndPositionsWithUserId(eq("user1"), eq("123")))
                .thenReturn(BalanceAndPositionsDto.builder()
                        .balance(AccountBalanceDto.builder().accountNo("123").totalBalance(BigDecimal.ZERO).build())
                        .positions(List.of())
                        .build());

        CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationByAccount(
                "user1", "123", null, null);

        assertThat(response.getSymbols()).isEmpty();
        assertThat(response.getMatrix()).isEmpty();
    }

    @Test
    @DisplayName("accountNo 기준 보유 1종목이면 빈 행렬 반환")
    void getCorrelationByAccount_onePosition_returnsEmptyMatrix() {
        when(accountService.getBalanceAndPositionsWithUserId(eq("user1"), eq("123")))
                .thenReturn(BalanceAndPositionsDto.builder()
                        .balance(AccountBalanceDto.builder().accountNo("123").totalBalance(BigDecimal.valueOf(1_000_000)).build())
                        .positions(List.of(
                                AccountPositionDto.builder().symbol("AAPL").market("US").totalValue(BigDecimal.valueOf(1_000_000)).build()))
                        .build());

        CorrelationAnalysisResponseDto response = correlationAnalysisService.getCorrelationByAccount(
                "user1", "123", null, null);

        assertThat(response.getSymbols()).isEmpty();
        assertThat(response.getMatrix()).isEmpty();
    }
}
