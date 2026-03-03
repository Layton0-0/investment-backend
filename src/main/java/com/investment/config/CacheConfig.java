package com.investment.config;

import org.springframework.beans.factory.annotation.Value;
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
 * - currentPrice: 실시간 현재가 (TTL은 investment.market-data.current-price-cache-ttl-seconds, 기본 300초. 단타/WebSocket 사용 시 5 등 짧게 설정 권장)
 * - analysis: 종목 분석 결과 (10분 TTL)
 * - account: 계좌 정보 (1분 TTL)
 *
 * 캐시 무효화: 주문 체결·계좌 갱신 시 해당 계좌/종목 캐시는 TTL 만료로 자동 갱신.
 * 강제 무효화가 필요하면 서비스에서 CacheManager.getCache(CACHE_ACCOUNT).evict(key) 등 호출.
 * Redis 미사용 시: spring.profiles.include=no-redis 로 캐시 타입 simple 사용 (로컬 개발용).
 */
@Configuration
@EnableCaching
public class CacheConfig {

        /**
         * 캐시 이름 상수
         */
        public static final String CACHE_MARKET_DATA = "marketData";
        /** 실시간 현재가 (종목별, 5분 TTL) */
        public static final String CACHE_CURRENT_PRICE = "currentPrice";
        public static final String CACHE_ANALYSIS = "analysis";
        public static final String CACHE_ACCOUNT = "account";
        /** 시스템 설정 (서버 기본값, 5분 TTL) */
        public static final String CACHE_SYSTEM_SETTINGS = "systemSettings";

        /**
         * Redis 캐시 매니저 설정
         * @param currentPriceCacheTtlSeconds 현재가 캐시 TTL(초). 단타/WebSocket 활성화 환경에서는 5 등 짧은 값 권장.
         */
        @Bean
        public CacheManager cacheManager(
                        RedisConnectionFactory redisConnectionFactory,
                        @Value("${investment.market-data.current-price-cache-ttl-seconds:300}") int currentPriceCacheTtlSeconds) {
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
                // 실시간 현재가: 프로퍼티 TTL (기본 300초, 단타/WebSocket 시 짧게 설정)
                cacheConfigurations.put(CACHE_CURRENT_PRICE, defaultConfig.entryTtl(Duration.ofSeconds(currentPriceCacheTtlSeconds)));

                // 종목 분석 결과: 10분 TTL
                cacheConfigurations.put(CACHE_ANALYSIS, defaultConfig.entryTtl(Duration.ofMinutes(10)));

                // 계좌 정보: 1분 TTL (자주 변경될 수 있음)
                cacheConfigurations.put(CACHE_ACCOUNT, defaultConfig.entryTtl(Duration.ofMinutes(1)));
                // 시스템 설정: 5분 TTL (관리자 변경 후 짧은 시간 내 반영)
                cacheConfigurations.put(CACHE_SYSTEM_SETTINGS, defaultConfig.entryTtl(Duration.ofMinutes(5)));

                return RedisCacheManager.builder(redisConnectionFactory)
                                .cacheDefaults(defaultConfig)
                                .withInitialCacheConfigurations(cacheConfigurations)
                                .build();
        }
}
