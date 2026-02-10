package com.investment.ops.service;

import com.investment.domain.entity.AlertLog;
import com.investment.domain.repository.AlertLogRepository;
import com.investment.ops.dto.AlertListResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpsAlertsService")
class OpsAlertsServiceTest {

    @Mock
    private AlertLogRepository alertLogRepository;

    @InjectMocks
    private OpsAlertsService opsAlertsService;

    @Test
    @DisplayName("getAlerts 레벨 없으면 전체 조회")
    void getAlerts_noLevel_returnsAll() {
        AlertLog log = AlertLog.of(Instant.now(), "WARNING", "UnfilledOrder", "test message");
        Page<AlertLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1L);
        when(alertLogRepository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, 20))).thenReturn(page);

        AlertListResponseDto result = opsAlertsService.getAlerts(0, 20, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getLevel()).isEqualTo("WARNING");
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getAlerts 레벨 있으면 필터 조회")
    void getAlerts_withLevel_filtersByLevel() {
        when(alertLogRepository.findByLevelOrderByOccurredAtDesc("ERROR", PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        AlertListResponseDto result = opsAlertsService.getAlerts(0, 20, "ERROR");

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }
}
