package com.investment.datacollection.service;

import com.investment.config.DataCollectionProperties;
import com.investment.domain.repository.DailyStockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsMarketCollectionService")
class UsMarketCollectionServiceTest {

    @Mock
    private DailyStockRepository dailyStockRepository;

    @Mock
    private DataCollectionProperties dataCollectionProperties;

    @InjectMocks
    private UsMarketCollectionService usMarketCollectionService;

    @Test
    @DisplayName("yfinance 스크립트 경로 미설정 시 0을 반환하고 저장하지 않는다")
    void collectAndSave_returnsZeroWhenScriptPathBlank() {
        // given: 스크립트 경로 미설정 (스텁 동작)
        when(dataCollectionProperties.getUs()).thenReturn(new DataCollectionProperties.Us());
        dataCollectionProperties.getUs().setYfinanceScriptPath("");

        LocalDate basDt = LocalDate.of(2026, 1, 30);

        // when
        int result = usMarketCollectionService.collectAndSave(basDt);

        // then
        assertEquals(0, result);
        verify(dailyStockRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("collectAndSave는 예외 없이 실행된다")
    void collectAndSave_executesWithoutException() {
        when(dataCollectionProperties.getUs()).thenReturn(new DataCollectionProperties.Us());
        dataCollectionProperties.getUs().setYfinanceScriptPath("");

        LocalDate basDt = LocalDate.of(2026, 1, 30);

        int result = usMarketCollectionService.collectAndSave(basDt);
        assertEquals(0, result);
    }
}
