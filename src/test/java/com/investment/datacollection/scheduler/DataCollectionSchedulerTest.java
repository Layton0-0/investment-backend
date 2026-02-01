package com.investment.datacollection.scheduler;

import com.investment.datacollection.service.DartCollectionService;
import com.investment.datacollection.service.KrxCollectionService;
import com.investment.datacollection.service.SecCollectionService;
import com.investment.datacollection.service.UsMarketCollectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataCollectionScheduler")
class DataCollectionSchedulerTest {

    @Mock
    private DartCollectionService dartCollectionService;

    @Mock
    private KrxCollectionService krxCollectionService;

    @Mock
    private SecCollectionService secCollectionService;

    @Mock
    private UsMarketCollectionService usMarketCollectionService;

    @InjectMocks
    private DataCollectionScheduler dataCollectionScheduler;

    @Test
    @DisplayName("collectUsDaily는 US 시장 수집 서비스를 호출한다")
    void collectUsDaily_callsUsMarketCollectionService() {
        // given
        when(usMarketCollectionService.collectAndSave(any(LocalDate.class))).thenReturn(0);

        // when
        dataCollectionScheduler.collectUsDaily();

        // then
        verify(usMarketCollectionService, times(1)).collectAndSave(any(LocalDate.class));
    }

    @Test
    @DisplayName("collectUsDaily는 예외 발생 시 로그만 남기고 계속 실행된다")
    void collectUsDaily_handlesExceptionGracefully() {
        // given
        when(usMarketCollectionService.collectAndSave(any(LocalDate.class)))
                .thenThrow(new RuntimeException("US 수집 실패"));

        // when & then - 예외가 발생해도 메서드가 정상 종료되어야 함
        dataCollectionScheduler.collectUsDaily();

        verify(usMarketCollectionService, times(1)).collectAndSave(any(LocalDate.class));
    }
}
