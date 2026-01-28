package com.investment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 보안 헤더 설정 필터
 * 
 * 다음 보안 헤더를 추가합니다:
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - X-XSS-Protection: 1; mode=block
 * - Strict-Transport-Security (HTTPS 환경)
 * - Content-Security-Policy
 */
@Component
@RequiredArgsConstructor
public class SecurityHeadersConfig extends OncePerRequestFilter {
    
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${SECURITY_HSTS_MAX_AGE:31536000}") // 기본 1년
    private long hstsMaxAge;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // X-Content-Type-Options: MIME 타입 스니핑 방지
        response.setHeader("X-Content-Type-Options", "nosniff");
        
        // X-Frame-Options: 클릭재킹 방지
        response.setHeader("X-Frame-Options", "DENY");
        
        // X-XSS-Protection: XSS 공격 방지 (구형 브라우저용)
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Strict-Transport-Security: HTTPS 강제 (HTTPS 환경에서만)
        if (sslEnabled || request.isSecure()) {
            response.setHeader("Strict-Transport-Security", 
                    String.format("max-age=%d; includeSubDomains", hstsMaxAge));
        }
        
        // Content-Security-Policy: XSS 및 데이터 주입 공격 방지
        // 기본 정책: 자신의 도메인에서만 리소스 로드 허용
        String csp = "default-src 'self'; " +
                     "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " + // 개발 편의를 위해 unsafe 허용 (프로덕션에서는 제거 권장)
                     "style-src 'self' 'unsafe-inline'; " +
                     "img-src 'self' data: https:; " +
                     "font-src 'self' data:; " +
                     "connect-src 'self'; " +
                     "frame-ancestors 'none';";
        response.setHeader("Content-Security-Policy", csp);
        
        // Referrer-Policy: 리퍼러 정보 제한
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions-Policy: 브라우저 기능 제한
        response.setHeader("Permissions-Policy", 
                "geolocation=(), microphone=(), camera=()");
        
        filterChain.doFilter(request, response);
    }
}
