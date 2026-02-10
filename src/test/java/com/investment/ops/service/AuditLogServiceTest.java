package com.investment.ops.service;

import com.investment.domain.entity.AuditLog;
import com.investment.domain.repository.AuditLogRepository;
import com.investment.ops.dto.AuditLogListResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    @DisplayName("record 호출 시 userId/accountNo 마스킹 후 저장")
    void record_masksAndSaves() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.record(AuditLogService.EVENT_SETTING_CHANGE, "user-123", "1234567890",
                "거래 설정 저장", AuditLogService.RESULT_SUCCESS, null);

        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuditLogService.EVENT_SETTING_CHANGE);
        assertThat(saved.getUserIdMasked()).isNotEqualTo("user-123");
        assertThat(saved.getAccountNoMasked()).isNotEqualTo("1234567890");
        assertThat(saved.getSummary()).isEqualTo("거래 설정 저장");
        assertThat(saved.getResult()).isEqualTo(AuditLogService.RESULT_SUCCESS);
    }

    @Test
    @DisplayName("findPage 필터 없으면 전체 조회")
    void findPage_noFilters_returnsAll() {
        AuditLog log = AuditLog.of(Instant.now(), AuditLogService.EVENT_MANUAL_TRIGGER, "ab***", null,
                "trigger path=/auto-buy", AuditLogService.RESULT_SUCCESS, null);
        Page<AuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1L);
        when(auditLogRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20)))).thenReturn(page);

        AuditLogListResponseDto result = auditLogService.findPage(0, 20, null, null, null);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getEventType()).isEqualTo(AuditLogService.EVENT_MANUAL_TRIGGER);
        assertThat(result.getTotalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("findPage eventType 필터 적용")
    void findPage_withEventType_filters() {
        when(auditLogRepository.findAll(any(Specification.class), eq(PageRequest.of(0, 20))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0L));

        AuditLogListResponseDto result = auditLogService.findPage(0, 20, "REAL_ACCOUNT_GUARD_BLOCKED", null, null);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0L);
    }
}
