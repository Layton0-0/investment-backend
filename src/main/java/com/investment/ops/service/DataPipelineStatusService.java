package com.investment.ops.service;

import com.investment.batch.service.BatchManagementService;
import com.investment.domain.repository.DailyStockRepository;
import com.investment.domain.repository.NewsItemRepository;
import com.investment.ops.dto.DataPipelineSourceStatusDto;
import com.investment.ops.dto.DataPipelineStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 데이터 파이프라인(원천별 수집) 상태 조회 서비스.
 * DART/SEC/KRX/US 원천별 마지막 실행 시각·최근 기준일·오류 요약을 제공.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPipelineStatusService {

    private static final String JOB_DART = "dart-disclosure-collector";
    private static final String JOB_SEC = "sec-disclosure-collector";
    private static final String JOB_KRX = "krx-daily-collector";
    private static final String JOB_US = "us-daily-collector";
    private static final String SOURCE_NEWS_DART = "DART";
    private static final String SOURCE_NEWS_SEC = "SEC_EDGAR";
    private static final String STATUS_OK = "OK";
    private static final String STATUS_WARNING = "WARNING";
    private static final String STATUS_ERROR = "ERROR";

    private final BatchManagementService batchManagementService;
    private final NewsItemRepository newsItemRepository;
    private final DailyStockRepository dailyStockRepository;

    /**
     * 원천별 수집 상태·최근 기준일·오류 요약 조회.
     */
    public DataPipelineStatusDto getStatus() {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        LocalDateTime updatedAt = LocalDateTime.now(zone);
        List<DataPipelineSourceStatusDto> sources = new ArrayList<>();

        // DART
        sources.add(buildSourceStatus(
                "DART",
                "DART 공시",
                JOB_DART,
                newsItemRepository.findMaxCollectedAtBySource(SOURCE_NEWS_DART).map(LocalDateTime::toLocalDate).orElse(null),
                updatedAt.toLocalDate()));

        // SEC
        sources.add(buildSourceStatus(
                "SEC",
                "SEC EDGAR 공시",
                JOB_SEC,
                newsItemRepository.findMaxCollectedAtBySource(SOURCE_NEWS_SEC).map(LocalDateTime::toLocalDate).orElse(null),
                updatedAt.toLocalDate()));

        // KRX
        sources.add(buildSourceStatus(
                "KRX",
                "KRX 일별 시세",
                JOB_KRX,
                dailyStockRepository.findMaxBasDtByMarket("KR").orElse(null),
                updatedAt.toLocalDate()));

        // US
        sources.add(buildSourceStatus(
                "US",
                "US 일별 시세",
                JOB_US,
                dailyStockRepository.findMaxBasDtByMarket("US").orElse(null),
                updatedAt.toLocalDate()));

        return DataPipelineStatusDto.builder()
                .sources(sources)
                .updatedAt(updatedAt)
                .build();
    }

    private DataPipelineSourceStatusDto buildSourceStatus(
            String sourceId,
            String displayName,
            String jobId,
            LocalDate lastBaselineDate,
            LocalDate today) {
        LocalDateTime lastRunTime = batchManagementService.getLastExecutionTimeForJob(jobId);
        String errorSummary = batchManagementService.getLastFailureMessage(jobId);
        String status = deriveStatus(errorSummary, lastBaselineDate, today, sourceId);
        return DataPipelineSourceStatusDto.builder()
                .sourceId(sourceId)
                .displayName(displayName)
                .lastRunTime(lastRunTime)
                .lastBaselineDate(lastBaselineDate)
                .status(status)
                .errorSummary(errorSummary)
                .build();
    }

    private String deriveStatus(String errorSummary, LocalDate lastBaselineDate, LocalDate today, String sourceId) {
        if (errorSummary != null && !errorSummary.isBlank()) {
            return STATUS_ERROR;
        }
        if (lastBaselineDate == null) {
            return STATUS_WARNING;
        }
        // 뉴스(DART/SEC)는 10분/15분 수집이므로 당일 데이터가 있으면 OK. 시세(KRX/US)는 장 마감 후 당일 또는 전일이면 OK.
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(lastBaselineDate, today);
        if (daysDiff <= 1) {
            return STATUS_OK;
        }
        if (daysDiff <= 3) {
            return STATUS_WARNING;
        }
        return STATUS_WARNING;
    }
}
