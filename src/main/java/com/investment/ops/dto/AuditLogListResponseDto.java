package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Ops 감사 로그 목록 페이징 응답 (GET /api/v1/ops/audit).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogListResponseDto {

    private List<AuditLogItemDto> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
