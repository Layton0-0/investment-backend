package com.investment.report.service;

import com.investment.account.dto.ProfitLossDto;
import com.investment.account.service.AccountService;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.report.dto.TaxReportSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("TaxReportService")
@ExtendWith(MockitoExtension.class)
class TaxReportServiceTest {

    @Mock
    private TradingSettingRepository tradingSettingRepository;

    @Mock
    private AccountService accountService;

    private TaxReportService taxReportService;

    @BeforeEach
    void setUp() {
        taxReportService = new TaxReportService(tradingSettingRepository, accountService);
    }

    @Test
    @DisplayName("getSummary userId null이면 스텁 반환")
    void getSummary_nullUserId_returnsStub() {
        TaxReportSummaryDto dto = taxReportService.getSummary(null, null);
        assertThat(dto.getYear()).isEqualTo(java.time.Year.now().getValue());
        assertThat(dto.getDisclaimer()).isNotBlank();
        assertThat(dto.getDomesticRealizedGainLoss()).isNull();
    }

    @Test
    @DisplayName("getSummary year null이면 현재 연도")
    void getSummary_nullYear_usesCurrentYear() {
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(Collections.emptyList());
        TaxReportSummaryDto dto = taxReportService.getSummary("user1", null);
        assertThat(dto.getYear()).isEqualTo(java.time.Year.now().getValue());
        assertThat(dto.getDisclaimer()).isNotBlank();
    }

    @Test
    @DisplayName("getSummary year 지정 시 해당 연도")
    void getSummary_withYear_returnsThatYear() {
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(Collections.emptyList());
        TaxReportSummaryDto dto = taxReportService.getSummary("user1", 2025);
        assertThat(dto.getYear()).isEqualTo(2025);
    }

    @Test
    @DisplayName("getSummary 100만원 실현손익 - 기본공제 250만원 적용 시 예상 세금 0원")
    void getSummary_withSmallGain_zeroTaxAfterDeduction() {
        TradingSetting s1 = TradingSetting.builder()
                .accountNo("acc1")
                .userId("user1")
                .maxInvestmentAmount(BigDecimal.ZERO)
                .minInvestmentAmount(BigDecimal.ZERO)
                .defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(List.of(s1));
        ProfitLossDto pl = ProfitLossDto.builder()
                .accountNo("acc1")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .totalProfitLoss(BigDecimal.ZERO)
                .totalProfitLossRate(BigDecimal.ZERO)
                .realizedProfitLoss(new BigDecimal("1000000")) // 100만원
                .unrealizedProfitLoss(BigDecimal.ZERO)
                .currency("KRW")
                .build();
        when(accountService.getPeriodProfitLoss(eq("acc1"), any(), any())).thenReturn(pl);

        TaxReportSummaryDto dto = taxReportService.getSummary("user1", 2025);
        assertThat(dto.getYear()).isEqualTo(2025);
        assertThat(dto.getDomesticRealizedGainLoss()).isEqualByComparingTo("1000000");
        assertThat(dto.getBasicDeduction()).isEqualByComparingTo("2500000"); // 250만원 공제
        assertThat(dto.getTaxableAmount()).isEqualByComparingTo("0"); // 과세대상 0
        assertThat(dto.getEstimatedTax()).isEqualByComparingTo("0"); // 세금 0
        assertThat(dto.getDisclaimer()).isNotBlank();
    }

    @Test
    @DisplayName("getSummary 500만원 실현손익 - 기본공제 250만원 적용 시 예상 세금 55만원")
    void getSummary_withLargeGain_taxCalculatedAfterDeduction() {
        TradingSetting s1 = TradingSetting.builder()
                .accountNo("acc1")
                .userId("user1")
                .maxInvestmentAmount(BigDecimal.ZERO)
                .minInvestmentAmount(BigDecimal.ZERO)
                .defaultCurrency("KRW")
                .build();
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(List.of(s1));
        ProfitLossDto pl = ProfitLossDto.builder()
                .accountNo("acc1")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .totalProfitLoss(BigDecimal.ZERO)
                .totalProfitLossRate(BigDecimal.ZERO)
                .realizedProfitLoss(new BigDecimal("5000000")) // 500만원
                .unrealizedProfitLoss(BigDecimal.ZERO)
                .currency("KRW")
                .build();
        when(accountService.getPeriodProfitLoss(eq("acc1"), any(), any())).thenReturn(pl);

        TaxReportSummaryDto dto = taxReportService.getSummary("user1", 2025);
        assertThat(dto.getYear()).isEqualTo(2025);
        assertThat(dto.getDomesticRealizedGainLoss()).isEqualByComparingTo("5000000");
        assertThat(dto.getBasicDeduction()).isEqualByComparingTo("2500000"); // 250만원 공제
        assertThat(dto.getTaxableAmount()).isEqualByComparingTo("2500000"); // 과세대상 250만원
        assertThat(dto.getEstimatedTax()).isEqualByComparingTo("550000"); // 250만 × 22% = 55만원
        assertThat(dto.getDisclaimer()).isNotBlank();
    }
}
