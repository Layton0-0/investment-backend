package com.investment.ops.service;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.AuditLog;
import com.investment.domain.repository.AuditLogRepository;
import com.investment.ops.dto.AuditLogItemDto;
import com.investment.ops.dto.AuditLogListResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Ops 감사 로그: 이벤트 기록·페이징 조회 (설정 변경·수동 트리거·실계좌 가드 차단).
 * userId/accountNo는 저장 시 마스킹.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.of("Asia/Seoul"));

    public static final String EVENT_SETTING_CHANGE = "SETTING_CHANGE";
    public static final String EVENT_MANUAL_TRIGGER = "MANUAL_TRIGGER";
    public static final String EVENT_REAL_ACCOUNT_GUARD_BLOCKED = "REAL_ACCOUNT_GUARD_BLOCKED";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";

    private final AuditLogRepository auditLogRepository;

    /**
     * 감사 이벤트 기록. userId/accountNo는 마스킹 후 저장.
     */
    @Transactional
    public void record(String eventType, String userId, String accountNo, String summary, String result,
            String ipAddress) {
        String userIdMasked = userId != null ? LogMaskingUtil.maskUserId(userId) : null;
        String accountNoMasked = accountNo != null ? LogMaskingUtil.maskAccountNo(accountNo) : null;
        AuditLog log = AuditLog.of(Instant.now(), eventType, userIdMasked, accountNoMasked, summary, result, ipAddress);
        auditLogRepository.save(log);
    }

    /**
     * 감사 로그 페이징 조회. eventType, from, to는 선택 필터.
     */
    @Transactional(readOnly = true)
    public AuditLogListResponseDto findPage(int page, int size, String eventType, Instant from, Instant to) {
        size = Math.min(Math.max(1, size), 100);
        Pageable pageable = PageRequest.of(page, size);

        Specification<AuditLog> spec = (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (eventType != null && !eventType.isBlank()) {
                predicates.add(cb.equal(root.get("eventType"), eventType.trim()));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            query.orderBy(cb.desc(root.get("occurredAt")));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);
        List<AuditLogItemDto> items = result.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return AuditLogListResponseDto.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    private AuditLogItemDto toDto(AuditLog log) {
        String occurredAt = log.getOccurredAt() != null
                ? ISO_FORMAT.format(log.getOccurredAt())
                : null;
        return AuditLogItemDto.builder()
                .id(log.getId())
                .occurredAt(occurredAt)
                .eventType(log.getEventType())
                .userIdMasked(log.getUserIdMasked())
                .accountNoMasked(log.getAccountNoMasked())
                .summary(log.getSummary())
                .result(log.getResult())
                .ipAddress(log.getIpAddress())
                .build();
    }
}
