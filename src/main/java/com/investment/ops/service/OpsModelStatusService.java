package com.investment.ops.service;

import com.investment.ai.client.AiPredictionClient;
import com.investment.ops.dto.OpsModelStatusDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Ops 모델/예측 상태 조회 서비스.
 * AiPredictionClient 기반 헬스 체크 결과를 DTO로 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpsModelStatusService {

    private final AiPredictionClient aiPredictionClient;

    @Value("${investment.ai.prediction-service.url:}")
    private String predictionServiceUrl;

    /**
     * 예측 서비스 상태 요약 조회.
     */
    public OpsModelStatusDto getStatus() {
        Instant now = Instant.now();
        String urlDisplay = (predictionServiceUrl != null && !predictionServiceUrl.isBlank())
                ? "configured"
                : "not configured";

        try {
            Boolean ready = aiPredictionClient.isModelReady()
                    .defaultIfEmpty(false)
                    .block();
            boolean modelReady = Boolean.TRUE.equals(ready);
            return OpsModelStatusDto.builder()
                    .modelReady(modelReady)
                    .serviceUrl(urlDisplay)
                    .lastCheckAt(now)
                    .build();
        } catch (Exception e) {
            log.warn("Ops 모델 상태 조회 중 오류: {}", e.getMessage());
            return OpsModelStatusDto.builder()
                    .modelReady(false)
                    .serviceUrl(urlDisplay)
                    .lastCheckAt(now)
                    .build();
        }
    }
}
