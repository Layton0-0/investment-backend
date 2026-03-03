package com.investment.governance;

import com.investment.alert.EmergencyAlertService;
import com.investment.backtest.BacktestService;
import com.investment.backtest.dto.BacktestRunRequest;
import com.investment.backtest.dto.BacktestRunResult;
import com.investment.config.GovernanceProperties;
import com.investment.domain.entity.GovernanceCheckResult;
import com.investment.domain.repository.GovernanceCheckResultRepository;
import com.investment.setting.service.SystemSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StrategyGovernanceCheckService")
class StrategyGovernanceCheckServiceTest {

    @Mock
    private BacktestService backtestService;
    @Mock
    private EmergencyAlertService emergencyAlertService;
    @Mock
    private GovernanceProperties governanceProperties;
    @Mock
    private GovernanceCheckResultRepository governanceCheckResultRepository;
    @Mock
    private GovernanceHaltService governanceHaltService;
    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private StrategyGovernanceCheckService strategyGovernanceCheckService;

    @Test
    @DisplayName("enabled false면 검사 스킵")
    void checkAndSendAlerts_disabled_skips() {
        when(systemSettingService.getBoolean("governance.enabled")).thenReturn(false);

        strategyGovernanceCheckService.checkAndSendAlerts();

        verify(backtestService, never()).run(any(BacktestRunRequest.class));
        verify(governanceCheckResultRepository, never()).save(any());
    }

    @Test
    @DisplayName("검사 후 결과 저장 및 열화 시 autoHaltOnDegradation true면 setHalt 호출")
    void checkAndSendAlerts_degraded_savesResultAndSetHalt() {
        when(systemSettingService.getBoolean("governance.enabled")).thenReturn(true);
        when(systemSettingService.getBoolean("governance.alertOnly")).thenReturn(false);
        when(systemSettingService.getBoolean("governance.autoHaltOnDegradation")).thenReturn(true);
        when(governanceProperties.getLookbackMonths()).thenReturn(12);
        when(governanceProperties.getDefaultCapital()).thenReturn(new BigDecimal("100000000"));
        when(governanceProperties.getMddThresholdPct()).thenReturn(new BigDecimal("-15"));
        when(governanceProperties.getSharpeMin()).thenReturn(BigDecimal.ZERO);

        BacktestRunResult degradedResult = BacktestRunResult.builder()
                .mddPct(new BigDecimal("-20"))
                .sharpeRatio(new BigDecimal("-0.1"))
                .build();
        when(backtestService.run(any(BacktestRunRequest.class))).thenReturn(degradedResult);

        strategyGovernanceCheckService.checkAndSendAlerts();

        ArgumentCaptor<GovernanceCheckResult> resultCaptor = ArgumentCaptor.forClass(GovernanceCheckResult.class);
        verify(governanceCheckResultRepository, atLeast(1)).save(resultCaptor.capture());
        List<GovernanceCheckResult> saved = resultCaptor.getAllValues();
        assertThat(saved).anyMatch(r -> "Y".equals(r.getDegraded()));

        verify(emergencyAlertService).sendRiskEventAlert(eq("WARNING"), eq("StrategyGovernance"), anyString());
        verify(governanceHaltService, atLeast(1)).setHalt(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("열화여도 alertOnly true면 setHalt 미호출")
    void checkAndSendAlerts_alertOnly_true_noSetHalt() {
        when(systemSettingService.getBoolean("governance.enabled")).thenReturn(true);
        when(systemSettingService.getBoolean("governance.alertOnly")).thenReturn(true);
        when(governanceProperties.getLookbackMonths()).thenReturn(12);
        when(governanceProperties.getDefaultCapital()).thenReturn(new BigDecimal("100000000"));
        when(governanceProperties.getMddThresholdPct()).thenReturn(new BigDecimal("-15"));
        when(governanceProperties.getSharpeMin()).thenReturn(BigDecimal.ZERO);
        // autoHaltOnDegradation is not read when alertOnly is true, so do not stub to avoid UnnecessaryStubbingException

        BacktestRunResult degradedResult = BacktestRunResult.builder()
                .mddPct(new BigDecimal("-20"))
                .sharpeRatio(BigDecimal.ZERO)
                .build();
        when(backtestService.run(any(BacktestRunRequest.class))).thenReturn(degradedResult);

        strategyGovernanceCheckService.checkAndSendAlerts();

        verify(governanceHaltService, never()).setHalt(anyString(), anyString(), anyString());
    }
}
