package com.investment.ops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.AuditLog;
import com.investment.domain.repository.AuditLogRepository;
import com.investment.ops.dto.AuditLogItemDto;
import com.investment.ops.dto.AuditLogListResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Ops 감사 로그: 이벤트 기록·페이징 조회 (설정 변경·수동 트리거·실계좌 가드 차단·트레이드 결정).
 * userId/accountNo는 저장 시 마스킹.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .withZone(ZoneId.of("Asia/Seoul"));

    public static final String EVENT_SETTING_CHANGE = "SETTING_CHANGE";
    public static final String EVENT_MANUAL_TRIGGER = "MANUAL_TRIGGER";
    public static final String EVENT_REAL_ACCOUNT_GUARD_BLOCKED = "REAL_ACCOUNT_GUARD_BLOCKED";
    /** P6-3: 매매 결정(매수/매도/스킵) 사유 기록 */
    public static final String EVENT_TRADE_DECISION = "TRADE_DECISION";

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILURE = "FAILURE";

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 감사 이벤트 기록. userId/accountNo는 마스킹 후 저장.
     */
    @Transactional
    public void record(String eventType, String userId, String accountNo, String summary, String result,
            String ipAddress) {
        record(eventType, userId, accountNo, summary, result, ipAddress, null);
    }

    /**
     * 감사 이벤트 기록 (상세 JSON 포함). 트레이드 저널 등에 사용.
     */
    @Transactional
    public void record(String eventType, String userId, String accountNo, String summary, String result,
            String ipAddress, String detailJson) {
        String userIdMasked = userId != null ? LogMaskingUtil.maskUserId(userId) : null;
        String accountNoMasked = accountNo != null ? LogMaskingUtil.maskAccountNo(accountNo) : null;
        AuditLog log = AuditLog.of(Instant.now(), eventType, userIdMasked, accountNoMasked, summary, result, ipAddress, detailJson);
        auditLogRepository.save(log);
    }

    /**
     * 트레이드 결정(매수/매도/스킵) 저널 기록. PipelineExecutor 등에서 호출.
     *
     * @param action   BUY, SELL, SKIP
     * @param reason   스킵 시 사유 (변동성 구간, 실계좌 가드 등)
     * @param regime   시장 레짐 (선택, 없으면 null)
     * @param riskGate 리스크 게이트 상태 요약 (선택)
     */
    public void logTradeDecision(String userId, String accountNo, String action, String symbol, String market,
            String strategyType, String factors, String regime, String riskGate, String result, String reason) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("action", action != null ? action : "UNKNOWN");
        detail.put("symbol", symbol);
        detail.put("market", market);
        detail.put("strategyType", strategyType);
        detail.put("factors", factors);
        detail.put("regime", regime);
        detail.put("riskGate", riskGate);
        detail.put("timestamp", Instant.now().toString());
        if (reason != null && !reason.isBlank()) {
            detail.put("reason", reason);
        }
        String detailJson;
        try {
            detailJson = objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            log.warn("트레이드 저널 detail JSON 직렬화 실패: {}", e.getMessage());
            detailJson = "{\"error\":\"serialization\"}";
        }
        String summary = String.format("%s %s %s %s - %s", action, symbol != null ? symbol : "", market != null ? market : "", strategyType != null ? strategyType : "", reason != null ? reason : result != null ? result : "");
        if (summary.length() > 500) {
            summary = summary.substring(0, 497) + "...";
        }
        record(EVENT_TRADE_DECISION, userId, accountNo, summary, result != null ? result : RESULT_SUCCESS, null, detailJson);
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
                .detailJson(log.getDetailJson())
                .build();
    }
}
