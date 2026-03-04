package com.investment.setting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 원클릭 자동투자 시작 요청. 초보자용 센서블 디폴트 적용.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickStartRequestDto {

    /** 최대 투자 금액 (원). 초보자 소액 권장. */
    @NotNull(message = "최대 투자 금액을 입력해 주세요")
    @DecimalMin(value = "0", inclusive = false)
    private BigDecimal maxInvestmentAmount;

    /** 적용할 계좌번호. 미설정 시 사용자 첫 계좌. */
    private String accountNo;
}
