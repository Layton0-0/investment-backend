package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ops 감사 로그 (TB_AUDIT_LOG).
 * 설정 변경·수동 트리거·실계좌 가드 차단 등 이벤트. userId/accountNo는 마스킹 후 저장.
 */
@Entity
@Table(name = "TB_AUDIT_LOG", indexes = {
        @Index(name = "IDX_TB_AUDIT_LOG_OCCURRED_AT", columnList = "OCCURRED_AT"),
        @Index(name = "IDX_TB_AUDIT_LOG_EVENT_TYPE", columnList = "EVENT_TYPE")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    @Column(name = "EVENT_TYPE", nullable = false, length = 64)
    private String eventType;

    @Column(name = "USER_ID_MASKED", length = 64)
    private String userIdMasked;

    @Column(name = "ACCOUNT_NO_MASKED", length = 32)
    private String accountNoMasked;

    @Column(name = "SUMMARY", length = 500)
    private String summary;

    @Column(name = "RESULT", length = 32)
    private String result;

    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    @Column(name = "CREATED_AT", nullable = false)
    private Instant createdAt;

    public static AuditLog of(Instant occurredAt, String eventType, String userIdMasked,
            String accountNoMasked, String summary, String result, String ipAddress) {
        AuditLog log = new AuditLog();
        log.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        log.eventType = eventType != null ? eventType : "UNKNOWN";
        log.userIdMasked = userIdMasked;
        log.accountNoMasked = accountNoMasked;
        log.summary = summary;
        log.result = result;
        log.ipAddress = ipAddress;
        log.createdAt = Instant.now();
        return log;
    }
}
