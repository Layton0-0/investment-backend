package com.investment.ops.service;

import com.investment.ai.client.AiPredictionClient;
import com.investment.ops.dto.OpsHealthDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;

/**
 * Ops 시스템 헬스 집계 서비스.
 * DB·Redis·예측 서비스 상태를 조회해 DTO로 반환.
 */
@Slf4j
@Service
public class OpsHealthService {

    private static final String UP = "UP";
    private static final String DOWN = "DOWN";
    private static final String UNKNOWN = "UNKNOWN";

    private final JdbcTemplate jdbcTemplate;
    private final AiPredictionClient aiPredictionClient;
    private final RedisTemplate<String, String> redisTemplateOptional;

    public OpsHealthService(
            JdbcTemplate jdbcTemplate,
            AiPredictionClient aiPredictionClient,
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.aiPredictionClient = aiPredictionClient;
        this.redisTemplateOptional = redisTemplate;
    }

    /**
     * DB·Redis·예측 서비스 상태 요약 조회.
     */
    public OpsHealthDto getHealth() {
        Instant now = Instant.now();
        String db = checkDb();
        String redis = checkRedis();
        String predictionService = checkPredictionService();
        return OpsHealthDto.builder()
                .db(db)
                .redis(redis)
                .predictionService(predictionService)
                .lastCheckedAt(now)
                .build();
    }

    private String checkDb() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return UP;
        } catch (Exception e) {
            log.warn("DB 헬스 체크 실패: {}", e.getMessage());
            return DOWN;
        }
    }

    private String checkRedis() {
        if (redisTemplateOptional == null) {
            return UNKNOWN;
        }
        try {
            redisTemplateOptional.getConnectionFactory().getConnection().ping();
            return UP;
        } catch (Exception e) {
            log.warn("Redis 헬스 체크 실패: {}", e.getMessage());
            return DOWN;
        }
    }

    private String checkPredictionService() {
        try {
            Boolean ready = aiPredictionClient.isModelReady().defaultIfEmpty(false).block();
            return Boolean.TRUE.equals(ready) ? UP : DOWN;
        } catch (Exception e) {
            log.warn("예측 서비스 헬스 체크 실패: {}", e.getMessage());
            return DOWN;
        }
    }
}
