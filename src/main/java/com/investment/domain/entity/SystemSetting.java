package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 서버 전역(시스템) 설정 엔티티.
 * 계정별 설정(TB_TRADING_SETTINGS)과 별개로, 서버 기본값만 저장.
 */
@Entity
@Table(name = "TB_SYSTEM_SETTINGS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @Column(name = "key", length = 100, nullable = false, unique = true)
    private String key;

    @Column(name = "value", columnDefinition = "TEXT")
    private String value;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    public void updateValue(String value, String updatedBy) {
        this.value = value;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = updatedBy;
    }
}
