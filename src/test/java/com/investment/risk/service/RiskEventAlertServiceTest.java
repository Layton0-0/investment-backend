package com.investment.risk.service;

import com.investment.alert.EmergencyAlertService;
import com.investment.config.RiskProperties;
import com.investment.domain.entity.TradingSetting;
import com.investment.domain.repository.TradingSettingRepository;
import com.investment.risk.dto.RiskAccountSummaryDto;
import com.investment.risk.dto.RiskSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEventAlertService")
class RiskEventAlertServiceTest {

    @Mock
    private EmergencyAlertService emergencyAlertService;
    @Mock
    private RiskReportService riskReportService;
    @Mock
    private RiskProperties riskProperties;
    @Mock
    private TradingSettingRepository tradingSettingRepository;

    @InjectMocks
    private RiskEventAlertService riskEventAlertService;

    @BeforeEach
    void setUp() {
        lenient().when(riskProperties.getAlertMddThresholdPct()).thenReturn(new BigDecimal("0.8"));
        lenient().when(riskProperties.getDailyLossLimitPct()).thenReturn(new BigDecimal("5"));
        lenient().when(riskProperties.isAlertVarExceedEnabled()).thenReturn(true);
    }

    @Test
    @DisplayName("checkAndSendAlerts 자동투자 설정 없으면 호출 스킵")
    void checkAndSendAlerts_noSettings_doesNotCallReport() {
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue()).thenReturn(List.of());

        riskEventAlertService.checkAndSendAlerts();

        verify(riskReportService, never()).getSummary(anyString());
        verify(emergencyAlertService, never()).sendRiskEventAlert(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("checkAndSendAlerts MDD·VaR 알림 모두 비활성화면 스킵")
    void checkAndSendAlerts_bothDisabled_doesNotCallReport() {
        when(riskProperties.getAlertMddThresholdPct()).thenReturn(null);
        when(riskProperties.isAlertVarExceedEnabled()).thenReturn(false);
        // 조기 return으로 repository 호출 없음 — lenient로 미사용 stubbing 허용
        lenient().when(tradingSettingRepository.findAllByAutoTradingEnabledTrue())
                .thenReturn(List.of(TradingSetting.builder().accountNo("acc1").userId("user1").build()));

        riskEventAlertService.checkAndSendAlerts();

        verify(riskReportService, never()).getSummary(anyString());
    }

    @Test
    @DisplayName("checkAndSendAlerts 일일 손실 한도 임박 시 WARNING 알림 발송")
    void checkAndSendAlerts_dailyLossApproaching_sendsWarning() {
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue())
                .thenReturn(List.of(TradingSetting.builder().accountNo("acc1").userId("user1").build()));
        RiskAccountSummaryDto account = RiskAccountSummaryDto.builder()
                .accountNoMasked("****1234")
                .serverType("1")
                .openingBalance(new BigDecimal("10000000"))
                .currentValue(new BigDecimal("9600000")) // 4% loss
                .build();
        RiskSummaryDto summary = RiskSummaryDto.builder()
                .killSwitchActive(false)
                .regimeGateEnabled(false)
                .riskGateAllowsNewBuy(true)
                .riskGateSizeMultiplier(BigDecimal.ONE)
                .accounts(List.of(account))
                .var95Pct(new BigDecimal("2")) // 4% > 2% so would also trigger VaR
                .build();
        when(riskReportService.getSummary("user1")).thenReturn(summary);
        // 4% / 5% = 0.8 >= 0.8 → DailyLossApproaching
        when(riskProperties.getAlertMddThresholdPct()).thenReturn(new BigDecimal("0.8"));

        riskEventAlertService.checkAndSendAlerts();

        ArgumentCaptor<String> levelCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> componentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(emergencyAlertService, times(2)).sendRiskEventAlert(levelCaptor.capture(), componentCaptor.capture(), messageCaptor.capture());
        assertThat(componentCaptor.getAllValues()).contains("DailyLossApproaching", "VarExceeded");
        assertThat(levelCaptor.getAllValues()).contains("WARNING", "ERROR");
        assertThat(messageCaptor.getValue()).contains("****1234");
    }

    @Test
    @DisplayName("checkAndSendAlerts 손실 없으면 알림 미발송")
    void checkAndSendAlerts_noLoss_doesNotSend() {
        when(tradingSettingRepository.findAllByAutoTradingEnabledTrue())
                .thenReturn(List.of(TradingSetting.builder().accountNo("acc1").userId("user1").build()));
        RiskAccountSummaryDto account = RiskAccountSummaryDto.builder()
                .accountNoMasked("****1234")
                .serverType("1")
                .openingBalance(new BigDecimal("10000000"))
                .currentValue(new BigDecimal("10200000")) // 수익
                .build();
        when(riskReportService.getSummary("user1")).thenReturn(RiskSummaryDto.builder()
                .killSwitchActive(false)
                .regimeGateEnabled(false)
                .riskGateAllowsNewBuy(true)
                .riskGateSizeMultiplier(BigDecimal.ONE)
                .accounts(List.of(account))
                .var95Pct(new BigDecimal("2"))
                .build());

        riskEventAlertService.checkAndSendAlerts();

        verify(emergencyAlertService, never()).sendRiskEventAlert(anyString(), anyString(), anyString());
    }
}
