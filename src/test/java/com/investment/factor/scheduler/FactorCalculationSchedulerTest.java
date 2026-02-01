package com.investment.factor.scheduler;

import com.investment.factor.service.FactorCalculationService;
import com.investment.factor.service.UniverseFilterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FactorCalculationScheduler")
class FactorCalculationSchedulerTest {

    @Mock
    private UniverseFilterService universeFilterService;

    @Mock
    private FactorCalculationService factorCalculationService;

    @InjectMocks
    private FactorCalculationScheduler factorCalculationScheduler;

    @Test
    @DisplayName("runFactorCalculation은 KR/US 시장 모두 처리한다")
    void runFactorCalculation_processesBothKrAndUsMarkets() {
        // given
        when(universeFilterService.run(any(LocalDate.class), eq("KR"))).thenReturn(10);
        when(universeFilterService.run(any(LocalDate.class), eq("US"))).thenReturn(5);
        when(factorCalculationService.calculateAndSave(any(LocalDate.class), eq("KR"))).thenReturn(100);
        when(factorCalculationService.calculateAndSave(any(LocalDate.class), eq("US"))).thenReturn(50);

        // when
        factorCalculationScheduler.runFactorCalculation();

        // then
        verify(universeFilterService, times(1)).run(any(LocalDate.class), eq("KR"));
        verify(universeFilterService, times(1)).run(any(LocalDate.class), eq("US"));
        verify(factorCalculationService, times(1)).calculateAndSave(any(LocalDate.class), eq("KR"));
        verify(factorCalculationService, times(1)).calculateAndSave(any(LocalDate.class), eq("US"));
    }

    @Test
    @DisplayName("KR 시장 실패해도 US 시장은 계속 처리한다")
    void runFactorCalculation_whenKrFails_continuesWithUs() {
        // given
        when(universeFilterService.run(any(LocalDate.class), eq("KR")))
                .thenThrow(new RuntimeException("KR 시장 처리 실패"));
        when(universeFilterService.run(any(LocalDate.class), eq("US"))).thenReturn(5);
        when(factorCalculationService.calculateAndSave(any(LocalDate.class), eq("US"))).thenReturn(50);

        // when
        factorCalculationScheduler.runFactorCalculation();

        // then
        verify(universeFilterService, times(1)).run(any(LocalDate.class), eq("KR"));
        verify(universeFilterService, times(1)).run(any(LocalDate.class), eq("US"));
        verify(factorCalculationService, never()).calculateAndSave(any(LocalDate.class), eq("KR"));
        verify(factorCalculationService, times(1)).calculateAndSave(any(LocalDate.class), eq("US"));
    }

    @Test
    @DisplayName("US 시장 실패해도 KR 시장은 계속 처리한다")
    void runFactorCalculation_whenUsFails_continuesWithKr() {
        // given
        when(universeFilterService.run(any(LocalDate.class), eq("KR"))).thenReturn(10);
        when(universeFilterService.run(any(LocalDate.class), eq("US")))
                .thenThrow(new RuntimeException("US 시장 처리 실패"));
        when(factorCalculationService.calculateAndSave(any(LocalDate.class), eq("KR"))).thenReturn(100);

        // when
        factorCalculationScheduler.runFactorCalculation();

        // then
        verify(universeFilterService, times(1)).run(any(LocalDate.class), eq("KR"));
        verify(universeFilterService, times(1)).run(any(LocalDate.class), eq("US"));
        verify(factorCalculationService, times(1)).calculateAndSave(any(LocalDate.class), eq("KR"));
        verify(factorCalculationService, never()).calculateAndSave(any(LocalDate.class), eq("US"));
    }
}
