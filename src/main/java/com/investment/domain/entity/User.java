package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 엔티티
 */
@Entity
@Table(name = "TB_USERS", indexes = {
    @Index(name = "IDX_USER_USERNAME", columnList = "USERNAME", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    
    @Id
    @Column(name = "TB_USERS_UID", length = 36, nullable = false, unique = true)
    private String id;
    
    @Column(name = "USERNAME", nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 비밀번호는 BCrypt로 해싱하여 저장합니다.
     * 평문 비밀번호는 절대 저장하지 않습니다.
     */
    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;
    
    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;
    
    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    @Column(name = "ROLE", length = 20, nullable = false)
    private String role = "User";

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @Builder
    public User(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role != null && !role.isBlank() ? role : "User";
    }
    
    /**
     * 비밀번호 업데이트
     */
    public void updatePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }
    
    /**
     * 마지막 로그인 시간 업데이트
     */
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
