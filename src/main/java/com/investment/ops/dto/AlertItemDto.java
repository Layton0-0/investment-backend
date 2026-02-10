package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ops 알림 이력 한 건 (GET /api/v1/ops/alerts 응답 항목).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertItemDto {

    private Long id;
    private String occurredAt;
    private String level;
    private String component;
    private String message;
}
