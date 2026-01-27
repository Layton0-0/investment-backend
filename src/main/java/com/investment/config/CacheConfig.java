package com.investment.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 캐싱 설정
 * 
 * 캐시 전략:
 * - marketData: 시장 데이터 (5분 TTL)
 * - analysis: 종목 분석 결과 (10분 TTL)
 * - account: 계좌 정보 (1분 TTL)
 */
@Configuration
@EnableCaching
public class CacheConfig {
    
    /**
     * 캐시 이름 상수
     */
    public static final String CACHE_MARKET_DATA = "marketData";
    public static final String CACHE_ANALYSIS = "analysis";
    public static final String CACHE_ACCOUNT = "account";
    
    /**
     * Redis 캐시 매니저 설정
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 기본 캐시 설정
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10)) // 기본 10분
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // null 값 캐싱 비활성화
        
        // 캐시별 TTL 설정
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 시장 데이터: 5분 TTL
        cacheConfigurations.put(CACHE_MARKET_DATA, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 종목 분석 결과: 10분 TTL
        cacheConfigurations.put(CACHE_ANALYSIS, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // 계좌 정보: 1분 TTL (자주 변경될 수 있음)
        cacheConfigurations.put(CACHE_ACCOUNT, defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
