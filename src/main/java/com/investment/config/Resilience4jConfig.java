package com.investment.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker 설정
 * 
 * 외부 API 호출 시 장애 격리 및 Fallback 전략 제공
 * 
 * Spring Boot 3.x에서는 application.yml의 resilience4j 설정을 통해 자동 구성됩니다.
 * 이 설정 클래스는 추가적인 커스터마이징이 필요한 경우에만 사용합니다.
 */
@Configuration
public class Resilience4jConfig {
    
    /**
     * Circuit Breaker Registry 설정
     * application.yml의 설정을 기본으로 사용하며, 필요시 추가 커스터마이징 가능
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
    
    /**
     * Circuit Breaker 기본 설정 (application.yml 설정과 함께 사용)
     * application.yml에 설정이 있으면 자동으로 적용되므로, 
     * 이 Bean은 추가 커스터마이징이 필요한 경우에만 사용합니다.
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
                .build();
    }
    
    /**
     * Time Limiter 설정 (타임아웃)
     * application.yml의 timelimiter 설정과 함께 사용
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
                // 타임아웃 시간 (5초)
                .timeoutDuration(Duration.ofSeconds(5))
                .build();
    }
}
