package com.investment.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 * 
 * 요청 헤더에서 JWT 토큰을 추출하여 인증 정보를 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            
            if (token != null && jwtTokenProvider.validateToken(token)) {
                String userId = jwtTokenProvider.getUserId(token);
                String username = jwtTokenProvider.getUsername(token);
                
                // 인증 정보 설정
                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                log.debug("JWT 인증 성공: userId={}, username={}", 
                        LogMaskingUtil.maskUserId(userId), 
                        LogMaskingUtil.maskUsername(username));
            }
        } catch (Exception e) {
            log.error("JWT 인증 필터 오류", e);
        }
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 요청에서 JWT 토큰 추출
     * 1. Authorization 헤더: Bearer <token> 형식 (API 요청용)
     * 2. 쿠키: token (웹 페이지 요청용)
     */
    private String extractToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 (API 요청용)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        // 2. 쿠키에서 토큰 추출 (웹 페이지 요청용)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("token".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        return token;
                    }
                }
            }
        }
        
        return null;
    }
}
