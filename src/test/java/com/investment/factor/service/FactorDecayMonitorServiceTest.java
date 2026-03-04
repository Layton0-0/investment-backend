package com.investment.factor.service;

import com.investment.alert.EmergencyAlertService;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.SignalScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactorDecayMonitorService")
class FactorDecayMonitorServiceTest {

    private static final LocalDate END = LocalDate.of(2026, 2, 1).minusDays(6);

    @Mock
    private SignalScoreRepository signalScoreRepository;
    @Mock
    private DailyStockRepository dailyStockRepository;
    @Mock
    private EmergencyAlertService emergencyAlertService;

    @InjectMocks
    private FactorDecayMonitorService factorDecayMonitorService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(factorDecayMonitorService, "lookbackMonths", 3);
        ReflectionTestUtils.setField(factorDecayMonitorService, "sharpeMin", 0.5);
    }

    @Test
    @DisplayName("시그널 없으면 알림 미발송")
    void checkAndSendAlerts_noSignals_noAlert() {
        when(signalScoreRepository.findByMarketAndBasDtBetween(eq("KR"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(signalScoreRepository.findByMarketAndBasDtBetween(eq("US"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        factorDecayMonitorService.checkAndSendAlerts();

        verify(emergencyAlertService, never()).sendRiskEventAlert(any(), any(), any());
    }

    @Test
    @DisplayName("getDegradedFactorTypes는 빈 목록 반환")
    void getDegradedFactorTypes_returnsEmpty() {
        assertThat(factorDecayMonitorService.getDegradedFactorTypes()).isEmpty();
    }
}
