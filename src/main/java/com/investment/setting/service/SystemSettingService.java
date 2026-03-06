package com.investment.setting.service;

import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.config.CacheConfig;
import com.investment.domain.entity.SystemSetting;
import com.investment.domain.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 서버 전역 설정 서비스.
 * DB 우선, 없으면 application.yml/Environment fallback.
 * 허용 키만 조회·저장(whitelist).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final Environment environment;

    /** Phase 1: 키, (yml property, 기본값 문자열). 활성/비활성 등 모든 설정은 Admin 화면에서 조회·저장. */
    private static final Map<String, KeyMeta> WHITELIST = new LinkedHashMap<>();
    static {
        WHITELIST.put("pipeline.autoExecute",
                new KeyMeta("investment.pipeline.auto-execute", "false", "Boolean", "파이프라인 자동 실행(서버 기본)"));
        WHITELIST.put("pipeline.allowRealExecution",
                new KeyMeta("investment.pipeline.allow-real-execution", "false", "Boolean", "실계좌 자동 실행 허용(서버 기본)"));
        WHITELIST.put("pipeline.scheduler.defaultCapital",
                new KeyMeta("investment.pipeline.scheduler.default-capital", "0", "BigDecimal", "스케줄러 기본 총자산(원)"));
        WHITELIST.put("pipeline.tradingWindowEnabled",
                new KeyMeta("investment.pipeline.trading-window.enabled", "true", "Boolean", "퀀트 매매 유리 시간대 사용 여부"));
        WHITELIST.put("governance.enabled",
                new KeyMeta("investment.governance.enabled", "true", "Boolean", "전략 거버넌스 검사 활성화"));
        WHITELIST.put("governance.alertOnly",
                new KeyMeta("investment.governance.alert-only", "true", "Boolean", "거버넌스 열화 시 알림만(true) / halt 가능(false)"));
        WHITELIST.put("governance.autoHaltOnDegradation",
                new KeyMeta("investment.governance.auto-halt-on-degradation", "false", "Boolean", "거버넌스 열화 시 자동 halt 등록"));
        WHITELIST.put("batch.failureAlertEnabled",
                new KeyMeta("investment.batch.failure-alert-enabled", "false", "Boolean", "배치 Job 실패 시 Discord 알림"));
        WHITELIST.put("risk.regimeGateEnabled",
                new KeyMeta("investment.risk.regime-gate-enabled", "false", "Boolean", "매크로 레짐 게이트(VIX 등) 신규 매수 차단"));
        WHITELIST.put("intraday.breakoutEnabled",
                new KeyMeta("investment.intraday.breakout-enabled", "false", "Boolean", "장중 변동성 돌파 진입 활성화"));
        WHITELIST.put("marketData.websocketEnabled",
                new KeyMeta("investment.market-data.korea-investment.websocket.enabled", "true", "Boolean", "WebSocket 실시간 연결 사용 여부"));
    }

    private static final Set<String> ALLOWED_KEYS = WHITELIST.keySet();

    @Cacheable(value = CacheConfig.CACHE_SYSTEM_SETTINGS, key = "#key", unless = "#result == null")
    @Transactional(readOnly = true)
    public Boolean getBoolean(String key) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE, "허용되지 않은 시스템 설정 키: " + key);
        }
        KeyMeta meta = WHITELIST.get(key);
        String raw = getRawValue(key);
        if (raw != null && !raw.isBlank()) {
            return Boolean.parseBoolean(raw.trim().toLowerCase(Locale.ROOT));
        }
        String fallback = environment.getProperty(meta.ymlProperty, meta.defaultValue);
        return Boolean.parseBoolean(fallback != null ? fallback : meta.defaultValue);
    }

    @Cacheable(value = CacheConfig.CACHE_SYSTEM_SETTINGS, key = "#key + '_bd'", unless = "#result == null")
    @Transactional(readOnly = true)
    public BigDecimal getBigDecimal(String key) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE, "허용되지 않은 시스템 설정 키: " + key);
        }
        KeyMeta meta = WHITELIST.get(key);
        String raw = getRawValue(key);
        if (raw != null && !raw.isBlank()) {
            try {
                return new BigDecimal(raw.trim());
            } catch (NumberFormatException e) {
                log.warn("시스템 설정 숫자 파싱 실패, yml fallback 사용: key={}, raw={}", key, raw);
            }
        }
        String fallback = environment.getProperty(meta.ymlProperty, meta.defaultValue);
        try {
            return new BigDecimal(fallback != null ? fallback : meta.defaultValue);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** DB에 저장된 원시 문자열만 반환. 없으면 null. */
    private String getRawValue(String key) {
        return systemSettingRepository.findById(key)
                .map(SystemSetting::getValue)
                .orElse(null);
    }

    /**
     * 허용된 모든 키에 대한 메타정보 + DB 값(있으면) 반환. 관리자 화면 목록용.
     */
    @Transactional(readOnly = true)
    public List<SystemSettingItemDto> listAll() {
        List<SystemSetting> stored = systemSettingRepository.findAllByKeyIn(ALLOWED_KEYS);
        Map<String, String> storedByKey = stored.stream()
                .collect(Collectors.toMap(SystemSetting::getKey, s -> s.getValue() != null ? s.getValue() : ""));
        return WHITELIST.entrySet().stream()
                .map(e -> {
                    KeyMeta meta = e.getValue();
                    String dbValue = storedByKey.get(e.getKey());
                    String effective = dbValue != null ? dbValue : environment.getProperty(meta.ymlProperty, meta.defaultValue);
                    return new SystemSettingItemDto(e.getKey(), meta.type, meta.description,
                            dbValue != null ? dbValue : null,
                            effective != null ? effective : meta.defaultValue);
                })
                .toList();
    }

    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_SYSTEM_SETTINGS, allEntries = true)
    public void put(String key, String value, String updatedBy) {
        if (!ALLOWED_KEYS.contains(key)) {
            throw new DomainException(ErrorCode.INVALID_SETTING_VALUE, "허용되지 않은 시스템 설정 키: " + key);
        }
        KeyMeta meta = WHITELIST.get(key);
        validateValue(key, meta.type, value);
        SystemSetting setting = systemSettingRepository.findById(key)
                .orElse(SystemSetting.builder()
                        .key(key)
                        .description(meta.description)
                        .build());
        setting.updateValue(value != null ? value.trim() : "", updatedBy);
        systemSettingRepository.save(setting);
        log.info("시스템 설정 저장: key={}, updatedBy={}", key, updatedBy);
    }

    /** 한 키에 대해 캐시 무효화 (다른 키의 BigDecimal 캐시는 key + "_bd" 이므로 함께 evict) */
    @CacheEvict(value = CacheConfig.CACHE_SYSTEM_SETTINGS, allEntries = true)
    public void evictCache() {
        // allEntries = true로 listAll/put 후 일괄 무효화 대신 사용 가능
    }

    private void validateValue(String key, String type, String value) {
        if (value == null || value.isBlank()) {
            return; // 빈 값이면 DB에 저장해 두고, 조회 시 fallback 사용
        }
        if ("Boolean".equals(type)) {
            String v = value.trim().toLowerCase(Locale.ROOT);
            if (!("true".equals(v) || "false".equals(v))) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                        "키 " + key + " 값은 true 또는 false 여야 합니다.");
            }
        } else if ("BigDecimal".equals(type)) {
            try {
                new BigDecimal(value.trim());
            } catch (NumberFormatException e) {
                throw new DomainException(ErrorCode.INVALID_SETTING_VALUE,
                        "키 " + key + " 값은 숫자여야 합니다.");
            }
        }
    }

    public static Set<String> getAllowedKeys() {
        return Collections.unmodifiableSet(ALLOWED_KEYS);
    }

    /** 관리자 API/화면용 DTO */
    public record SystemSettingItemDto(String key, String type, String description, String valueFromDb, String effectiveValue) {}

    private record KeyMeta(String ymlProperty, String defaultValue, String type, String description) {}
}
