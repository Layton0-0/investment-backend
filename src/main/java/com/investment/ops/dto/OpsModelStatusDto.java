package com.investment.ops.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ops 모델/예측 상태 응답.
 * Admin /ops/model 화면용.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpsModelStatusDto {

    /** 예측 서비스 모델 사용 가능 여부 */
    private boolean modelReady;
    /** 예측 서비스 URL 표시 (마스킹: configured / not configured) */
    private String serviceUrl;
    /** 마지막 헬스 체크 시각 */
    private Instant lastCheckAt;
    /** 모델/서비스 버전 (구현 시) */
    private String version;
    /** 최근 예측 실패율 0~1 (구현 시) */
    private Double failureRateRecent;
}
