package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ops 시스템 헬스 응답.
 * Admin /ops/health 화면용. DB·Redis·예측 서비스 상태 요약.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpsHealthDto {

    /** DB 상태: UP, DOWN */
    private String db;
    /** Redis 상태: UP, DOWN, UNKNOWN(미설정) */
    private String redis;
    /** 예측 서비스 상태: UP, DOWN, UNKNOWN */
    private String predictionService;
    /** 마지막 체크 시각 */
    private Instant lastCheckedAt;
}
