package com.investment.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 보안 이벤트 감사 로깅 서비스
 * 
 * 보안 관련 이벤트를 구조화된 JSON 형식으로 로깅합니다.
 * - 인증 성공/실패
 * - 권한 위반
 * - 비정상 행동 감지
 * - 계정 잠금/해제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final ObjectMapper objectMapper;

    /**
     * 보안 이벤트 타입
     */
    public enum SecurityEventType {
        AUTHENTICATION_SUCCESS,
        AUTHENTICATION_FAILURE,
        AUTHORIZATION_FAILURE,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        PASSWORD_CHANGED,
        API_KEY_CHANGED,
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_EXCEEDED,
        TOKEN_EXPIRED,
        TOKEN_INVALID
    }

    /**
     * 보안 이벤트 로깅
     * 
     * @param eventType 이벤트 타입
     * @param userId    사용자 ID (마스킹됨)
     * @param username  사용자명 (마스킹됨)
     * @param details   추가 정보
     */
    public void logSecurityEvent(SecurityEventType eventType, String userId, String username,
            Map<String, Object> details) {
        try {
            Map<String, Object> auditLog = new HashMap<>();
            auditLog.put("timestamp", LocalDateTime.now().toString());
            auditLog.put("eventType", eventType.name());
            auditLog.put("userId", userId != null ? LogMaskingUtil.maskUserId(userId) : null);
            auditLog.put("username", username != null ? LogMaskingUtil.maskUsername(username) : null);

            if (details != null) {
                // details의 값들도 마스킹 처리
                Map<String, Object> maskedDetails = new HashMap<>();
                for (Map.Entry<String, Object> entry : details.entrySet()) {
                    Object value = entry.getValue();
                    String key = entry.getKey().toLowerCase();

                    // 민감한 정보 마스킹
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (key.contains("password") || key.contains("pwd") || key.contains("pass")) {
                            value = LogMaskingUtil.maskPassword(strValue);
                        } else if (key.contains("token") || key.contains("apikey") || key.contains("secret")) {
                            value = LogMaskingUtil.maskToken(strValue);
                        } else if (key.contains("email")) {
                            value = LogMaskingUtil.maskEmail(strValue);
                        } else if (key.contains("userid") || key.contains("user_id")) {
                            value = LogMaskingUtil.maskUserId(strValue);
                        } else if (key.contains("username") || key.contains("user_name")) {
                            value = LogMaskingUtil.maskUsername(strValue);
                        }
                    }
                    maskedDetails.put(entry.getKey(), value);
                }
                auditLog.put("details", maskedDetails);
            }

            String jsonLog = objectMapper.writeValueAsString(auditLog);
            log.info("[SECURITY_AUDIT] {}", jsonLog);
            if (log.isDebugEnabled()) {
                log.debug("  [DEBUG] eventType={}, userId(actual)={}, username(actual)={}", eventType, userId,
                        username);
            }

        } catch (JsonProcessingException e) {
            log.error("보안 이벤트 로깅 실패: eventType={}, userId={}", eventType, userId, e);
        }
    }

    /**
     * 인증 성공 로깅
     */
    public void logAuthenticationSuccess(String userId, String username, String ipAddress) {
        Map<String, Object> details = new HashMap<>();
        details.put("ipAddress", ipAddress);
        logSecurityEvent(SecurityEventType.AUTHENTICATION_SUCCESS, userId, username, details);
    }

    /**
     * 인증 실패 로깅
     */
    public void logAuthenticationFailure(String username, String reason, String ipAddress) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        details.put("ipAddress", ipAddress);
        logSecurityEvent(SecurityEventType.AUTHENTICATION_FAILURE, null, username, details);
    }

    /**
     * 권한 위반 로깅
     */
    public void logAuthorizationFailure(String userId, String username, String resource, String action) {
        Map<String, Object> details = new HashMap<>();
        details.put("resource", resource);
        details.put("action", action);
        logSecurityEvent(SecurityEventType.AUTHORIZATION_FAILURE, userId, username, details);
    }

    /**
     * 계정 잠금 로깅
     */
    public void logAccountLocked(String userId, String username, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        logSecurityEvent(SecurityEventType.ACCOUNT_LOCKED, userId, username, details);
    }

    /**
     * 계정 잠금 해제 로깅
     */
    public void logAccountUnlocked(String userId, String username) {
        logSecurityEvent(SecurityEventType.ACCOUNT_UNLOCKED, userId, username, null);
    }

    /**
     * 비밀번호 변경 로깅
     */
    public void logPasswordChanged(String userId, String username) {
        logSecurityEvent(SecurityEventType.PASSWORD_CHANGED, userId, username, null);
    }

    /**
     * API 키 변경 로깅
     */
    public void logApiKeyChanged(String userId, String username) {
        logSecurityEvent(SecurityEventType.API_KEY_CHANGED, userId, username, null);
    }

    /**
     * 비정상 행동 감지 로깅
     */
    public void logSuspiciousActivity(String userId, String username, String activity, String ipAddress) {
        Map<String, Object> details = new HashMap<>();
        details.put("activity", activity);
        details.put("ipAddress", ipAddress);
        logSecurityEvent(SecurityEventType.SUSPICIOUS_ACTIVITY, userId, username, details);
    }

    /**
     * Rate Limit 초과 로깅
     */
    public void logRateLimitExceeded(String identifier, String endpoint, String ipAddress) {
        Map<String, Object> details = new HashMap<>();
        details.put("identifier", identifier);
        details.put("endpoint", endpoint);
        details.put("ipAddress", ipAddress);
        logSecurityEvent(SecurityEventType.RATE_LIMIT_EXCEEDED, identifier, null, details);
    }

    /**
     * 토큰 만료 로깅
     */
    public void logTokenExpired(String userId, String username) {
        logSecurityEvent(SecurityEventType.TOKEN_EXPIRED, userId, username, null);
    }

    /**
     * 토큰 무효 로깅
     */
    public void logTokenInvalid(String userId, String username, String reason) {
        Map<String, Object> details = new HashMap<>();
        details.put("reason", reason);
        logSecurityEvent(SecurityEventType.TOKEN_INVALID, userId, username, details);
    }
}
