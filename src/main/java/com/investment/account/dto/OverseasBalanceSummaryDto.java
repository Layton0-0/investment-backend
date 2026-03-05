package com.investment.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 해외주식 체결기준현재잔고 API output2 기반 요약.
 * 미국(840) 외화(02) 기준 예수금·총자산 등 대시보드 US 계좌 카드용.
 *
 * @see <a href="https://github.com/koreainvestment/open-trading-api">한국투자증권 open-trading-api</a>
 *      inquire_present_balance output2: tot_dncl_amt(총예수금액), tot_asst_amt(총자산금액)
 */
@Schema(description = "해외(미국) 계좌 잔고 요약 (예수금·총자산)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverseasBalanceSummaryDto {

    @Schema(description = "예수금 (외화 기준, output2 tot_dncl_amt)")
    private BigDecimal deposit;

    @Schema(description = "총 자산 금액 (output2 tot_asst_amt)")
    private BigDecimal totalAsset;

    @Schema(description = "총 평가손익 (output2 tot_evlu_pfls_amt)")
    private BigDecimal totalProfitLoss;

    @Schema(description = "총 평가수익률 (output2 evlu_erng_rt1 등)")
    private BigDecimal totalProfitLossRate;

    @Schema(description = "통화 코드 (USD)")
    private String currency;
}
