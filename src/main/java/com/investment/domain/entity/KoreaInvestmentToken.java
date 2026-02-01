package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 한국투자증권 Access Token 엔티티
 * 
 * 서버 시작 시 발급받은 Access Token을 암호화하여 저장합니다.
 * 토큰은 만료 시간이 있으므로 만료 전에 갱신해야 합니다.
 */
@Entity
@Table(name = "TB_KOREA_INVESTMENT_TOKENS", indexes = {
    @Index(name = "IDX_KI_TOKEN_USER_ID", columnList = "USER_ID"),
    @Index(name = "UK_KI_TOKENS_USER_SERVER", columnList = "USER_ID,SERVER_TYPE", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KoreaInvestmentToken {
    
    @Id
    @Column(name = "TB_KI_TOKENS_UID", length = 36, nullable = false, unique = true)
    private String id;
    
    @Column(name = "USER_ID", nullable = false, length = 36)
    private String userId;

    /**
     * 서버 타입 (모의투자: "1", 실거래: "0")
     */
    @Column(name = "SERVER_TYPE", nullable = false, length = 1)
    private String serverType = "1";

    /**
     * Access Token (암호화하여 저장)
     */
    @Column(name = "ACCESS_TOKEN_ENCRYPTED", nullable = false, columnDefinition = "TEXT")
    private String accessTokenEncrypted;
    
    /**
     * 토큰 만료 시간 (밀리초 타임스탬프)
     */
    @Column(name = "EXPIRES_AT", nullable = false)
    private Long expiresAt;
    
    /**
     * 토큰 발급 시간
     */
    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (issuedAt == null) {
            issuedAt = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Builder
    public KoreaInvestmentToken(String userId, String serverType, String accessTokenEncrypted,
                               Long expiresAt, LocalDateTime issuedAt) {
        this.userId = userId;
        this.serverType = serverType != null ? serverType : "1";
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.expiresAt = expiresAt;
        this.issuedAt = issuedAt != null ? issuedAt : LocalDateTime.now();
    }
    
    /**
     * 토큰 갱신
     */
    public void updateToken(String accessTokenEncrypted, Long expiresAt) {
        this.accessTokenEncrypted = accessTokenEncrypted;
        this.expiresAt = expiresAt;
        this.issuedAt = LocalDateTime.now();
    }
    
    /**
     * 토큰이 유효한지 확인 (만료 시간 체크)
     */
    public boolean isValid() {
        return expiresAt != null && System.currentTimeMillis() < expiresAt;
    }
}
