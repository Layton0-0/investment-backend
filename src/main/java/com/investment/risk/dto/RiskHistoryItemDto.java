package com.investment.risk.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 리스크 이력 항목 (게이트 축소·손실 한도 도달 등).
 * 1차는 저장 구조 없음으로 빈 목록 반환용 스키마만 정의.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskHistoryItemDto {

    private String eventType;
    private String accountNoMasked;
    private String description;
    private Instant occurredAt;
}
