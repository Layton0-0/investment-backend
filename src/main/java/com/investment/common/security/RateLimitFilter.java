package com.investment.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * API Rate Limiting 필터
 * 
 * - 인증 API: IP 기반 rate limiting (분당 5회)
 * - 일반 API: 사용자 기반 rate limiting (분당 100회)
 * - Redis 기반 분산 rate limiting
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final SecurityAuditService securityAuditService;
    
    @Value("${RATE_LIMIT_AUTH_ENABLED:true}")
    private boolean authRateLimitEnabled;
    
    @Value("${RATE_LIMIT_AUTH_REQUESTS_PER_MINUTE:5}")
    private int authRequestsPerMinute;
    
    @Value("${RATE_LIMIT_API_ENABLED:true}")
    private boolean apiRateLimitEnabled;
    
    @Value("${RATE_LIMIT_API_REQUESTS_PER_MINUTE:100}")
    private int apiRequestsPerMinute;
    
    // 인증 API 경로
    private static final String[] AUTH_PATHS = {"/api/v1/auth/login", "/api/v1/auth/signup"};
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        String path = request.getRequestURI();
        String method = request.getMethod();
        
        // OPTIONS 요청은 rate limiting 제외
        if ("OPTIONS".equals(method)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // 인증 API인 경우 IP 기반 rate limiting
            if (isAuthPath(path) && authRateLimitEnabled) {
                String clientIp = HttpRequestUtil.getClientIpAddress(request);
                String key = "rate_limit:auth:" + clientIp;
                
                if (!checkRateLimit(key, authRequestsPerMinute, 60)) {
                    securityAuditService.logRateLimitExceeded(clientIp, path, clientIp);
                    response.setStatus(429); // HTTP 429 Too Many Requests
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.\",\"retryAfter\":60}");
                    return;
                }
            }
            
            // 일반 API인 경우 사용자 기반 rate limiting
            if (path.startsWith("/api/") && !isAuthPath(path) && apiRateLimitEnabled) {
                String userId = getUserIdFromRequest(request);
                if (userId != null) {
                    String key = "rate_limit:api:" + userId;
                    
                    if (!checkRateLimit(key, apiRequestsPerMinute, 60)) {
                        String clientIp = HttpRequestUtil.getClientIpAddress(request);
                        securityAuditService.logRateLimitExceeded(userId, path, clientIp);
                        response.setStatus(429); // HTTP 429 Too Many Requests
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write("{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.\",\"retryAfter\":60}");
                        return;
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Rate limiting 오류", e);
            // Rate limiting 오류가 발생해도 요청은 계속 진행
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 인증 API 경로인지 확인
     */
    private boolean isAuthPath(String path) {
        for (String authPath : AUTH_PATHS) {
            if (path.equals(authPath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 요청에서 사용자 ID 추출
     */
    private String getUserIdFromRequest(HttpServletRequest request) {
        // Spring Security 컨텍스트에서 사용자 ID 가져오기
        try {
            org.springframework.security.core.Authentication authentication = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            // 인증 정보가 없을 수 있음 (정상)
        }
        return null;
    }
    
    /**
     * Rate limit 체크
     * 
     * @param key Redis 키
     * @param maxRequests 최대 요청 수
     * @param timeWindowSeconds 시간 윈도우 (초)
     * @return true: 허용, false: 초과
     */
    private boolean checkRateLimit(String key, int maxRequests, int timeWindowSeconds) {
        try {
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            String currentCount = ops.get(key);
            
            if (currentCount == null) {
                // 첫 요청
                ops.set(key, "1", timeWindowSeconds, TimeUnit.SECONDS);
                return true;
            }
            
            int count = Integer.parseInt(currentCount);
            if (count >= maxRequests) {
                // Rate limit 초과
                return false;
            }
            
            // 카운트 증가
            ops.increment(key);
            return true;
            
        } catch (Exception e) {
            log.error("Rate limit 체크 오류: key={}", key, e);
            // 오류 발생 시 허용 (fail-open)
            return true;
        }
    }
}
