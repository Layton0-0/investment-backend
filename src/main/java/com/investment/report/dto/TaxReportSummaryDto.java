package com.investment.report.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 연말 세금·리포트 요약 (스텁).
 * 기획요청 §9: 연간 실현손익·국내/해외 구분·배당·예상 세금.
 * 실제 PDF/CSV·Hometax 연동은 후속 스프린트.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaxReportSummaryDto {

    /** 기준 연도 */
    private Integer year;
    /** 국내 실현 손익 (원). 스텁 시 null */
    private BigDecimal domesticRealizedGainLoss;
    /** 해외 실현 손익 (원, 원화 환산). 스텁 시 null */
    private BigDecimal overseasRealizedGainLoss;
    /** 배당 소득 요약 (원). 스텁 시 null */
    private BigDecimal dividendTotal;
    /** 연간 기본공제 (원). 국내주식 250만원 */
    private BigDecimal basicDeduction;
    /** 과세대상 금액 (원). 실현손익 - 기본공제. 음수 시 0 */
    private BigDecimal taxableAmount;
    /** 예상 세금(추정, 원). 스텁 시 null */
    private BigDecimal estimatedTax;
    /** 가정·한계·비세무자문 면책 문구 */
    private String disclaimer;
}
