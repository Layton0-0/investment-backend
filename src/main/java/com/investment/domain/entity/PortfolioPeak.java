package com.investment.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 계좌별 평가액 피크. PreTradeCompliance MDD(15%) 검사용.
 */
@Entity
@Table(name = "TB_PORTFOLIO_PEAK")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PortfolioPeak {

    @Id
    @Column(name = "ACCOUNT_NO", nullable = false, length = 20)
    private String accountNo;

    @Column(name = "PEAK_VALUE", nullable = false, precision = 18, scale = 2)
    private BigDecimal peakValue;

    @Column(name = "PEAK_DATE", nullable = false)
    private LocalDate peakDate;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static PortfolioPeak of(String accountNo, BigDecimal peakValue, LocalDate peakDate) {
        PortfolioPeak p = new PortfolioPeak();
        p.accountNo = accountNo;
        p.peakValue = peakValue;
        p.peakDate = peakDate;
        p.updatedAt = LocalDateTime.now();
        return p;
    }

    public void updateIfHigher(BigDecimal currentValue, LocalDate asOfDate) {
        if (currentValue != null && currentValue.compareTo(peakValue) > 0) {
            this.peakValue = currentValue;
            this.peakDate = asOfDate;
        }
    }
}
