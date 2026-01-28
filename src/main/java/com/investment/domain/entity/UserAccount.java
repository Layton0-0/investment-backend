package com.investment.domain.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자 계좌 엔티티
 * 
 * 사용자의 증권사 계좌 정보를 암호화하여 저장합니다.
 * - 사용자 (1) : 증권사 (N) - 한 사용자가 여러 증권사 사용 가능
 * - 증권사 (1) : 계좌 (N) - 한 증권사에 여러 계좌 보유 가능
 * - 한국투자증권: API 키 (1) : 계좌 (1) - 계좌별로 API 키 발급
 * - 다른 증권사: API 키 (1) : 계좌 (N) - 증권사별 하나의 API 키로 여러 계좌 관리 가능
 */
@Entity
@Table(name = "TB_USER_ACCOUNTS", indexes = {
        @Index(name = "IDX_USER_ACCOUNTS_USER_ID", columnList = "USER_ID"),
        @Index(name = "IDX_USER_ACCOUNTS_USER_API_KEY", columnList = "USER_API_KEY_ID"),
        @Index(name = "IDX_USER_ACCOUNTS_BROKER_TYPE", columnList = "BROKER_TYPE"),
        @Index(name = "IDX_USER_ACCOUNTS_IS_DEFAULT", columnList = "USER_ID,IS_DEFAULT"),
        @Index(name = "UK_USER_ACCOUNTS_USER_ACCOUNT", columnList = "USER_ID,ACCOUNT_NO_ENCRYPTED,BROKER_TYPE", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    @Id
    @Column(name = "TB_USER_ACCOUNTS_UID", length = 36, nullable = false, unique = true)
    private String id;

    /**
     * 계좌 소유자
     */
    @Column(name = "USER_ID", nullable = false, length = 36)
    private String userId;

    /**
     * 계좌에 사용할 API 키 (증권사별)
     * 한국투자증권의 경우: 계좌별로 API 키가 발급되므로 1:1 관계
     * 다른 증권사의 경우: 증권사별 하나의 API 키로 여러 계좌 관리 가능 (1:N)
     */
    @Column(name = "USER_API_KEY_ID", nullable = false, length = 36)
    private String userApiKeyId;

    /**
     * 암호화된 계좌번호
     */
    @Column(name = "ACCOUNT_NO_ENCRYPTED", nullable = false, columnDefinition = "TEXT")
    private String accountNoEncrypted;

    /**
     * 증권사 코드 (계좌가 속한 증권사)
     * UserApiKey의 BROKER_TYPE과 일치해야 함
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "BROKER_TYPE", nullable = false, length = 50)
    private BrokerType brokerType;

    /**
     * 계좌 별칭 (예: "한국투자증권 ****1234")
     */
    @Column(name = "ACCOUNT_NAME", length = 100)
    private String accountName;

    /**
     * 메인 계좌 여부 (사용자의 메인 계좌 지정, 모든 증권사 통합 기준)
     * 사용자당 메인 계좌는 1개만 가능
     */
    @Column(name = "IS_DEFAULT", nullable = false)
    private Boolean isDefault = false;

    /**
     * 활성화 여부
     */
    @Column(name = "IS_ACTIVE", nullable = false)
    private Boolean isActive = true;

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
    public UserAccount(String userId, String userApiKeyId, String accountNoEncrypted,
            BrokerType brokerType, String accountName, Boolean isDefault, Boolean isActive) {
        this.userId = userId;
        this.userApiKeyId = userApiKeyId;
        this.accountNoEncrypted = accountNoEncrypted;
        this.brokerType = brokerType;
        this.accountName = accountName;
        this.isDefault = isDefault != null ? isDefault : false;
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * 메인 계좌로 설정
     */
    public void setAsDefault() {
        this.isDefault = true;
    }

    /**
     * 메인 계좌 해제
     */
    public void unsetAsDefault() {
        this.isDefault = false;
    }

    /**
     * 계좌 별칭 업데이트
     */
    public void updateAccountName(String accountName) {
        this.accountName = accountName;
    }

    /**
     * 활성화 상태 변경
     */
    public void setActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }
}
