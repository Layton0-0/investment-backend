package com.investment.risk.service;

import com.investment.common.security.EncryptionUtil;
import com.investment.config.RiskProperties;
import com.investment.domain.entity.PortfolioPeak;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.repository.PortfolioPeakRepository;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.factor.service.DailyLossLimitService;
import com.investment.factor.service.RiskGateService;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.RiskLimitsDto;
import com.investment.risk.dto.RiskSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskReportService")
class RiskReportServiceTest {

    @Mock
    private TradingHaltService tradingHaltService;
    @Mock
    private RiskGateService riskGateService;
    @Mock
    private DailyLossLimitService dailyLossLimitService;
    @Mock
    private PortfolioPeakRepository portfolioPeakRepository;
    @Mock
    private RiskProperties riskProperties;
    @Mock
    private TradingSettingRepository tradingSettingRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private RiskReportService riskReportService;

    @BeforeEach
    void setUp() {
        lenient().when(riskProperties.getVixThreshold()).thenReturn(new BigDecimal("30"));
        lenient().when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("50"));
        lenient().when(riskProperties.getDailyLossLimitPct()).thenReturn(new BigDecimal("5"));
        lenient().when(riskProperties.getVarDailyVolPct()).thenReturn(null);
    }

    @Test
    @DisplayName("getSummary 계좌 없으면 빈 목록")
    void getSummary_noAccounts_returnsEmptyAccounts() {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(false);
        when(riskGateService.evaluate(null)).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(List.of());

        RiskSummaryDto dto = riskReportService.getSummary("user1");

        assertThat(dto.isKillSwitchActive()).isFalse();
        assertThat(dto.getAccounts()).isEmpty();
    }

    @Test
    @DisplayName("getSummary 계좌 있으면 요약 반환")
    void getSummary_withAccount_returnsAccountSummary() {
        when(tradingHaltService.isHaltAllOrders()).thenReturn(false);
        when(riskGateService.evaluate(null)).thenReturn(RiskGateService.RiskGateResult.allow(BigDecimal.ONE));
        TradingSetting setting = mock(TradingSetting.class);
        when(setting.getAccountNo()).thenReturn("12345678");
        when(tradingSettingRepository.findByUserIdOrderByAccountNo("user1")).thenReturn(List.of(setting));
        UserAccount userAccount = mock(UserAccount.class);
        when(userAccount.getAccountNoEncrypted()).thenReturn("enc123");
        when(userAccount.getServerType()).thenReturn("1");
        when(userAccountRepository.findByUserId("user1")).thenReturn(List.of(userAccount));
        when(encryptionUtil.decrypt("enc123")).thenReturn("12345678");
        when(dailyLossLimitService.getOpeningBalance(eq("12345678"), any(LocalDate.class))).thenReturn(new BigDecimal("10000000"));
        when(dailyLossLimitService.getCurrentPortfolioValue("12345678")).thenReturn(new BigDecimal("10500000"));
        when(dailyLossLimitService.isNewBuyAllowed("12345678")).thenReturn(true);
        when(portfolioPeakRepository.findByAccountNo("12345678"))
                .thenReturn(Optional.of(PortfolioPeak.of("12345678", new BigDecimal("11000000"), LocalDate.now())));

        RiskSummaryDto dto = riskReportService.getSummary("user1");

        assertThat(dto.getAccounts()).hasSize(1);
        assertThat(dto.getAccounts().get(0).getCurrentValue()).isEqualByComparingTo(new BigDecimal("10500000"));
        assertThat(dto.getAccounts().get(0).isNewBuyBlockedByDailyLoss()).isFalse();
        assertThat(dto.getAccounts().get(0).getMdd()).isNotNull();
    }

    @Test
    @DisplayName("getLimits RiskProperties 기반 반환")
    void getLimits_returnsFromProperties() {
        when(riskProperties.isRegimeGateEnabled()).thenReturn(true);
        when(riskProperties.getVixThreshold()).thenReturn(new BigDecimal("28"));
        when(riskProperties.getReduceSizeOnHighVolPct()).thenReturn(new BigDecimal("40"));
        when(riskProperties.getDailyLossLimitPct()).thenReturn(new BigDecimal("3"));

        RiskLimitsDto dto = riskReportService.getLimits();

        assertThat(dto.isRegimeGateEnabled()).isTrue();
        assertThat(dto.getVixThreshold()).isEqualByComparingTo(new BigDecimal("28"));
        assertThat(dto.getDailyLossLimitPct()).isEqualByComparingTo(new BigDecimal("3"));
    }

    @Test
    @DisplayName("getHistory 1차 빈 목록")
    void getHistory_returnsEmpty() {
        List<?> list = riskReportService.getHistory("user1", LocalDate.now().minusDays(7), LocalDate.now());
        assertThat(list).isEmpty();
    }
}
