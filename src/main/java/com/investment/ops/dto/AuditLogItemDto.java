package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Ops 감사 로그 한 건 (GET /api/v1/ops/audit 응답 항목).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogItemDto {

    private Long id;
    private String occurredAt;
    private String eventType;
    private String userIdMasked;
    private String accountNoMasked;
    private String summary;
    private String result;
    private String ipAddress;
}
