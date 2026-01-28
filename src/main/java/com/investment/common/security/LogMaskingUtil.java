package com.investment.common.security;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Pattern;

/**
 * 로그에서 PII(개인식별정보) 및 민감 정보 마스킹 유틸리티
 * 
 * 보안 규칙에 따라 로그에 민감한 정보가 노출되지 않도록 마스킹합니다.
 */
@Slf4j
public class LogMaskingUtil {
    
    // 마스킹 패턴: 앞 2자리만 표시, 나머지는 *로 대체
    private static final int VISIBLE_PREFIX_LENGTH = 2;
    private static final int MIN_LENGTH_FOR_MASKING = 4;
    
    // 토큰/API 키 마스킹: 앞 4자리만 표시
    private static final int TOKEN_VISIBLE_LENGTH = 4;
    
    // 이메일 마스킹 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"
    );
    
    // UUID 패턴 (36자리)
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );
    
    /**
     * 사용자 ID 마스킹
     * UUID 형식인 경우 앞 8자리만 표시
     */
    public static String maskUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return "***";
        }
        
        // UUID 형식인 경우
        if (UUID_PATTERN.matcher(userId).matches()) {
            return userId.substring(0, 8) + "-****-****-****-************";
        }
        
        // 일반 문자열인 경우
        if (userId.length() <= VISIBLE_PREFIX_LENGTH) {
            return "***";
        }
        
        return userId.substring(0, VISIBLE_PREFIX_LENGTH) + 
               "*".repeat(Math.min(userId.length() - VISIBLE_PREFIX_LENGTH, 20));
    }
    
    /**
     * 사용자명 마스킹
     */
    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "***";
        }
        
        if (username.length() <= VISIBLE_PREFIX_LENGTH) {
            return "***";
        }
        
        return username.substring(0, VISIBLE_PREFIX_LENGTH) + 
               "*".repeat(Math.min(username.length() - VISIBLE_PREFIX_LENGTH, 20));
    }
    
    /**
     * 이메일 마스킹
     * 예: user@example.com -> us**@ex******.com
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }
        
        java.util.regex.Matcher matcher = EMAIL_PATTERN.matcher(email);
        if (matcher.matches()) {
            String localPart = matcher.group(1);
            String domain = matcher.group(2);
            
            String maskedLocal = maskString(localPart, VISIBLE_PREFIX_LENGTH);
            String maskedDomain = maskString(domain, VISIBLE_PREFIX_LENGTH);
            
            return maskedLocal + "@" + maskedDomain;
        }
        
        // 이메일 형식이 아닌 경우 일반 마스킹
        return maskString(email, VISIBLE_PREFIX_LENGTH);
    }
    
    /**
     * 토큰/API 키 마스킹
     * 앞 4자리만 표시
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "***";
        }
        
        if (token.length() <= TOKEN_VISIBLE_LENGTH) {
            return "***";
        }
        
        return token.substring(0, TOKEN_VISIBLE_LENGTH) + 
               "*".repeat(Math.min(token.length() - TOKEN_VISIBLE_LENGTH, 30));
    }
    
    /**
     * API 키 마스킹 (토큰과 동일)
     */
    public static String maskApiKey(String apiKey) {
        return maskToken(apiKey);
    }
    
    /**
     * 비밀번호 마스킹 (항상 완전히 마스킹)
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "***";
        }
        
        return "****";
    }
    
    /**
     * 일반 문자열 마스킹
     * 
     * @param value 마스킹할 문자열
     * @param visibleLength 앞에 표시할 문자 수
     * @return 마스킹된 문자열
     */
    public static String maskString(String value, int visibleLength) {
        if (value == null || value.isEmpty()) {
            return "***";
        }
        
        if (value.length() <= visibleLength) {
            return "***";
        }
        
        if (value.length() < MIN_LENGTH_FOR_MASKING) {
            return "*".repeat(value.length());
        }
        
        return value.substring(0, visibleLength) + 
               "*".repeat(Math.min(value.length() - visibleLength, 20));
    }
    
    /**
     * 로그 메시지에서 민감 정보 자동 마스킹
     * 
     * @param message 원본 로그 메시지
     * @return 마스킹된 로그 메시지
     */
    public static String maskLogMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        
        String masked = message;
        
        // UUID 패턴 마스킹
        masked = UUID_PATTERN.matcher(masked).replaceAll(matchResult -> {
            String uuid = matchResult.group();
            return maskUserId(uuid);
        });
        
        // 이메일 패턴 마스킹
        masked = EMAIL_PATTERN.matcher(masked).replaceAll(matchResult -> {
            return maskEmail(matchResult.group());
        });
        
        // 일반적인 패턴들 (password=, token=, apiKey= 등)
        masked = Pattern.compile("(?i)(password|pwd|pass)\\s*[:=]\\s*([^\\s,}]+)", Pattern.CASE_INSENSITIVE)
                .matcher(masked)
                .replaceAll(matchResult -> matchResult.group(1) + "=" + maskPassword(matchResult.group(2)));
        
        masked = Pattern.compile("(?i)(token|apikey|apisecret|secret)\\s*[:=]\\s*([^\\s,}]+)", Pattern.CASE_INSENSITIVE)
                .matcher(masked)
                .replaceAll(matchResult -> matchResult.group(1) + "=" + maskToken(matchResult.group(2)));
        
        return masked;
    }
}
