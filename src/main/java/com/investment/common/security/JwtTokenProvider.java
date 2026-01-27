package com.investment.common.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증 유틸리티
 */
@Slf4j
@Component
public class JwtTokenProvider {
    
    private final SecretKey secretKey;
    private final long tokenValidityInMilliseconds;
    
    public JwtTokenProvider(
            @Value("${investment.security.jwt-secret:default-secret-key-change-in-production-minimum-256-bits}") String jwtSecret,
            @Value("${investment.security.jwt-expiration:86400000}") long tokenValidityInMilliseconds) {
        // JWT Secret을 SecretKey로 변환 (최소 256비트 필요)
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
    }
    
    /**
     * JWT 토큰 생성
     * 
     * @param userId 사용자 ID
     * @param username 사용자명
     * @return JWT 토큰
     */
    public String createToken(String userId, String username) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + tokenValidityInMilliseconds);
        
        return Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }
    
    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    public String getUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.getSubject();
    }
    
    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("username", String.class);
    }
    
    /**
     * JWT 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 토큰 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}
