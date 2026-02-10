package com.investment.batch.service;

import com.investment.batch.dto.BatchJobDto;
import com.investment.batch.registry.BatchJobDefinition;
import com.investment.batch.registry.BatchJobRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchManagementService")
class BatchManagementServiceTest {

    @Mock
    private BatchJobRegistry batchJobRegistry;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BatchManagementService batchManagementService;

    @Test
    @DisplayName("getAllBatchJobs 레지스트리 기반 목록 및 JobRepository 집계 반환")
    void getAllBatchJobs_returnsNonEmptyList() {
        when(batchJobRegistry.getDefinitions()).thenReturn(List.of(
                BatchJobDefinition.builder()
                        .id("trading-portfolio-generator")
                        .name("트레이딩 포트폴리오 생성")
                        .description("매일 09:00 생성")
                        .cronExpression("0 0 9 * * *")
                        .timeZone("Asia/Seoul")
                        .triggerPath("/api/v1/trading-portfolios/generate")
                        .build(),
                BatchJobDefinition.builder()
                        .id("krx-daily-collector")
                        .name("KRX 일별 시세 수집")
                        .description("매일 16:00")
                        .cronExpression("0 0 16 * * *")
                        .timeZone("Asia/Seoul")
                        .triggerPath("/api/v1/trigger/krx-daily")
                        .build()));
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), anyString())).thenReturn(0L);
        when(jdbcTemplate.queryForObject(anyString(), eq(java.sql.Timestamp.class), anyString())).thenReturn(null);

        List<BatchJobDto> jobs = batchManagementService.getAllBatchJobs();

        assertNotNull(jobs);
        assertEquals(2, jobs.size());
        assertTrue(jobs.stream().anyMatch(j -> "trading-portfolio-generator".equals(j.getId())));
        assertTrue(jobs.stream().anyMatch(j -> "krx-daily-collector".equals(j.getId())));
        jobs.forEach(job -> {
            assertNotNull(job.getId());
            assertNotNull(job.getName());
            assertNotNull(job.getStatus());
            assertNotNull(job.getCronDescription());
            assertNotNull(job.getExecutionCount());
            assertNotNull(job.getSuccessCount());
            assertNotNull(job.getFailureCount());
        });
    }
}
