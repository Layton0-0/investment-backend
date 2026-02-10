package com.investment.ops.service;

import com.investment.batch.service.BatchManagementService;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.ops.dto.DataPipelineStatusDto;
import com.investment.ops.dto.DataPipelineSourceStatusDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataPipelineStatusService")
class DataPipelineStatusServiceTest {

    @Mock
    private BatchManagementService batchManagementService;
    @Mock
    private NewsItemRepository newsItemRepository;
    @Mock
    private DailyStockRepository dailyStockRepository;

    @InjectMocks
    private DataPipelineStatusService dataPipelineStatusService;

    @Test
    @DisplayName("getStatus 원천 4개(DART/SEC/KRX/US) 및 updatedAt 반환")
    void getStatus_returnsFourSourcesAndUpdatedAt() {
        when(batchManagementService.getLastExecutionTimeForJob(anyString())).thenReturn(LocalDateTime.now().minusHours(1));
        when(batchManagementService.getLastFailureMessage(anyString())).thenReturn(null);
        when(newsItemRepository.findMaxCollectedAtBySource(eq("DART"))).thenReturn(Optional.of(LocalDateTime.now()));
        when(newsItemRepository.findMaxCollectedAtBySource(eq("SEC_EDGAR"))).thenReturn(Optional.of(LocalDateTime.now()));
        when(dailyStockRepository.findMaxBasDtByMarket(eq("KR"))).thenReturn(Optional.of(LocalDate.now()));
        when(dailyStockRepository.findMaxBasDtByMarket(eq("US"))).thenReturn(Optional.of(LocalDate.now()));

        DataPipelineStatusDto result = dataPipelineStatusService.getStatus();

        assertThat(result).isNotNull();
        assertThat(result.getSources()).hasSize(4);
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getSources().stream().map(DataPipelineSourceStatusDto::getSourceId))
                .containsExactlyInAnyOrder("DART", "SEC", "KRX", "US");
        assertThat(result.getSources().stream().map(DataPipelineSourceStatusDto::getStatus))
                .containsOnly("OK");
    }

    @Test
    @DisplayName("getStatus 실패 메시지 있으면 해당 원천 status ERROR")
    void getStatus_whenFailureMessage_thenStatusError() {
        when(batchManagementService.getLastExecutionTimeForJob(anyString())).thenReturn(LocalDateTime.now());
        when(batchManagementService.getLastFailureMessage(eq("dart-disclosure-collector"))).thenReturn("Connection timeout");
        when(batchManagementService.getLastFailureMessage(eq("sec-disclosure-collector"))).thenReturn(null);
        when(batchManagementService.getLastFailureMessage(eq("krx-daily-collector"))).thenReturn(null);
        when(batchManagementService.getLastFailureMessage(eq("us-daily-collector"))).thenReturn(null);
        when(newsItemRepository.findMaxCollectedAtBySource(anyString())).thenReturn(Optional.of(LocalDateTime.now()));
        when(dailyStockRepository.findMaxBasDtByMarket(anyString())).thenReturn(Optional.of(LocalDate.now()));

        DataPipelineStatusDto result = dataPipelineStatusService.getStatus();

        assertThat(result.getSources()).filteredOn(s -> "DART".equals(s.getSourceId()))
                .singleElement()
                .satisfies(s -> {
                    assertThat(s.getStatus()).isEqualTo("ERROR");
                    assertThat(s.getErrorSummary()).isEqualTo("Connection timeout");
                });
    }
}
