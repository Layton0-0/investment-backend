package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Ops 알림 이력 (TB_ALERT_LOG).
 * Discord 긴급 알림 발송 시 동일 내용 저장.
 */
@Entity
@Table(name = "TB_ALERT_LOG", indexes = {
        @Index(name = "IDX_TB_ALERT_LOG_OCCURRED_AT", columnList = "OCCURRED_AT"),
        @Index(name = "IDX_TB_ALERT_LOG_LEVEL", columnList = "LEVEL")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    @Column(name = "LEVEL", nullable = false, length = 20)
    private String level;

    @Column(name = "COMPONENT", length = 100)
    private String component;

    @Column(name = "MESSAGE", columnDefinition = "TEXT")
    private String message;

    public static AlertLog of(Instant occurredAt, String level, String component, String message) {
        AlertLog log = new AlertLog();
        log.occurredAt = occurredAt != null ? occurredAt : Instant.now();
        log.level = level != null ? level : "INFO";
        log.component = component;
        log.message = message;
        return log;
    }
}
