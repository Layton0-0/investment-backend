package com.investment.batch.service;

import com.investment.batch.dto.BatchJobDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BatchManagementService")
class BatchManagementServiceTest {

    @InjectMocks
    private BatchManagementService batchManagementService;

    @Test
    @DisplayName("getAllBatchJobs 배치 작업 목록 반환")
    void getAllBatchJobs_returnsNonEmptyList() {
        List<BatchJobDto> jobs = batchManagementService.getAllBatchJobs();

        assertNotNull(jobs);
        assertFalse(jobs.isEmpty());
        assertTrue(jobs.stream().anyMatch(j -> "trading-portfolio-generator".equals(j.getId())));
        assertTrue(jobs.stream().anyMatch(j -> "short-term-strategy-executor".equals(j.getId())));
        jobs.forEach(job -> {
            assertNotNull(job.getId());
            assertNotNull(job.getName());
            assertNotNull(job.getStatus());
        });
    }
}
