package com.investment.ops.service;

import com.investment.domain.entity.AlertLog;
import com.investment.domain.repository.AlertLogRepository;
import com.investment.ops.dto.AlertItemDto;
import com.investment.ops.dto.AlertListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ops 알림센터: 알림 이력 조회 (페이징·필터).
 */
@Service
@RequiredArgsConstructor
public class OpsAlertsService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.of("Asia/Seoul"));

    private final AlertLogRepository alertLogRepository;

    /**
     * 알림 이력 페이징 조회. level이 있으면 해당 레벨만 필터.
     */
    public AlertListResponseDto getAlerts(int page, int size, String level) {
        size = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertLog> result = level != null && !level.isBlank()
                ? alertLogRepository.findByLevelOrderByOccurredAtDesc(level.trim(), pageable)
                : alertLogRepository.findAllByOrderByOccurredAtDesc(pageable);

        List<AlertItemDto> items = result.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return AlertListResponseDto.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private AlertItemDto toDto(AlertLog log) {
        String occurredAt = log.getOccurredAt() != null
                ? ISO_FORMAT.format(log.getOccurredAt())
                : null;
        return AlertItemDto.builder()
                .id(log.getId())
                .occurredAt(occurredAt)
                .level(log.getLevel())
                .component(log.getComponent())
                .message(log.getMessage())
                .build();
    }
}
