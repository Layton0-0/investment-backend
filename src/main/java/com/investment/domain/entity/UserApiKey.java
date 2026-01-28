package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 API 키 엔티티
 * 
 * 사용자의 증권사 API 키 정보를 암호화하여 저장합니다.
 * - 사용증권명: BrokerType enum으로 코드화
 * - App Key: 암호화하여 저장
 * - App Secret: 암호화하여 저장
 */
@Entity
@Table(name = "TB_USER_API_KEYS", indexes = {
        @Index(name = "IDX_USER_API_KEY_USER_ID", columnList = "USER_ID"),
        @Index(name = "IDX_USER_API_KEY_BROKER_TYPE", columnList = "BROKER_TYPE")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserApiKey {

    @Id
    @Column(name = "TB_USER_API_KEYS_UID", length = 36, nullable = false, unique = true)
    private String id;

    @Column(name = "USER_ID", nullable = false, length = 36)
    private String userId;

    /**
     * 사용증권명 (코드화)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "BROKER_TYPE", nullable = false, length = 50)
    private BrokerType brokerType;

    /**
     * App Key (암호화하여 저장)
     */
    @Column(name = "APP_KEY_ENCRYPTED", nullable = false, columnDefinition = "TEXT")
    private String appKeyEncrypted;

    /**
     * App Secret (암호화하여 저장)
     */
    @Column(name = "APP_SECRET_ENCRYPTED", nullable = false, columnDefinition = "TEXT")
    private String appSecretEncrypted;

    /**
     * 서버 타입 (모의투자: "1", 실거래: "0")
     */
    @Column(name = "SERVER_TYPE", nullable = false, length = 1)
    private String serverType = "1"; // 기본: 모의투자

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public UserApiKey(String userId, BrokerType brokerType,
            String appKeyEncrypted, String appSecretEncrypted, String serverType) {
        this.userId = userId;
        this.brokerType = brokerType;
        this.appKeyEncrypted = appKeyEncrypted;
        this.appSecretEncrypted = appSecretEncrypted;
        this.serverType = serverType != null ? serverType : "1";
    }

    /**
     * API 키 정보 업데이트
     */
    public void updateApiKeys(String appKeyEncrypted, String appSecretEncrypted, String serverType) {
        this.appKeyEncrypted = appKeyEncrypted;
        this.appSecretEncrypted = appSecretEncrypted;
        if (serverType != null) {
            this.serverType = serverType;
        }
    }
}
