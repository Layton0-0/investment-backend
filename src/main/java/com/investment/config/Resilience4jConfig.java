package com.investment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker 설정
 * 
 * 외부 API 호출 시 장애 격리 및 Fallback 전략 제공
 */
@Configuration
public class Resilience4jConfig {
    
    /**
     * Circuit Breaker 기본 설정
     */
    @Bean
    public CircuitBreakerConfig circuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                // 실패율 임계값 (50%)
                .failureRateThreshold(50)
                // 최소 호출 횟수 (10회)
                .minimumNumberOfCalls(10)
                // 슬라이딩 윈도우 크기 (100회)
                .slidingWindowSize(100)
                // Circuit Breaker 열림 상태 유지 시간 (60초)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                // 반열림 상태에서 허용할 호출 수 (5회)
                .permittedNumberOfCallsInHalfOpenState(5)
                // 슬라이딩 윈도우 타입 (카운트 기반)
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                // 기록된 예외가 실패로 간주되는지 여부
                .recordExceptions(Exception.class)
                // 무시할 예외
                .ignoreExceptions()
                .build();
    }
    
    /**
     * Time Limiter 설정 (타임아웃)
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                // 타임아웃 시간 (5초)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }
    
    /**
     * Circuit Breaker Factory 설정
     */
    @Bean
    public Resilience4JCircuitBreakerFactory resilience4JCircuitBreakerFactory(
            CircuitBreakerConfig circuitBreakerConfig,
            TimeLimiterConfig timeLimiterConfig) {
        
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                CircuitBreakerRegistry.ofDefaults(),
                null,
                null);
        
        factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(circuitBreakerConfig)
                .timeLimiterConfig(timeLimiterConfig)
                .build());
        
        return factory;
    }
}
