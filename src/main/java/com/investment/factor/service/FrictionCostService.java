package com.investment.factor.service;

import com.investment.config.FrictionCostProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 매매 판단·손익 계산 시 사용하는 왕복 비용(수수료·세금·슬리피지) 서비스.
 * 모든 득실 계산은 이 비용을 반영한 순손익(net) 기준으로 한다.
 *
 * @see com.investment.config.FrictionCostProperties
 * @see docs/02-architecture/00-strategy-registry.md §2.10 Friction cost
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FrictionCostService {

    private final FrictionCostProperties frictionCostProperties;

    /**
     * 시장별 주식 왕복 비용률 (매수+매도 수수료·세금·슬리피지, 비율).
     * KR: 2×위탁수수료 + 증권거래세(매도) + 2×슬리피지.
     * US: 2×위탁수수료 + SEC Fee + 2×슬리피지 (TAF는 수량 기준이라 별도 금액으로).
     *
     * @param market KR, US
     * @return 0.002 = 0.2% 등
     */
    public BigDecimal getRoundTripCostRate(String market) {
        if (market == null) {
            return BigDecimal.ZERO;
        }
        if ("US".equalsIgnoreCase(market)) {
            FrictionCostProperties.UsaStockFee usa = frictionCostProperties.getUsa().getStock();
            BigDecimal c = usa.getCommission() != null ? usa.getCommission() : BigDecimal.ZERO;
            BigDecimal s = usa.getSlippage() != null ? usa.getSlippage() : BigDecimal.ZERO;
            BigDecimal sec = usa.getSecFee() != null ? usa.getSecFee() : BigDecimal.ZERO;
            return c.multiply(BigDecimal.valueOf(2)).add(s.multiply(BigDecimal.valueOf(2))).add(sec);
        }
        FrictionCostProperties.StockFee kr = frictionCostProperties.getKorea().getStock();
        BigDecimal c = kr.getCommission() != null ? kr.getCommission() : BigDecimal.ZERO;
        BigDecimal t = kr.getTax() != null ? kr.getTax() : BigDecimal.ZERO;
        BigDecimal s = kr.getSlippage() != null ? kr.getSlippage() : BigDecimal.ZERO;
        return c.multiply(BigDecimal.valueOf(2)).add(t).add(s.multiply(BigDecimal.valueOf(2)));
    }

    /**
     * 주문 금액·수량에 대한 왕복 비용 금액 (원화 또는 USD 환산).
     * KR: notional × roundTripRate.
     * US: notional × roundTripRate + TAF(수량×주당 TAF).
     *
     * @param market   KR, US
     * @param notional 거래 금액 (원 또는 USD)
     * @param quantity 수량 (US 시 TAF 계산용)
     * @return 왕복 비용 금액
     */
    public BigDecimal getRoundTripCostAmount(String market, BigDecimal notional, int quantity) {
        if (notional == null || notional.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal rate = getRoundTripCostRate(market);
        BigDecimal cost = notional.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        if ("US".equalsIgnoreCase(market) && quantity > 0) {
            FrictionCostProperties.UsaStockFee usa = frictionCostProperties.getUsa().getStock();
            BigDecimal tafPerShare = usa.getTafPerShareUsd() != null ? usa.getTafPerShareUsd() : BigDecimal.ZERO;
            BigDecimal taf = tafPerShare.multiply(BigDecimal.valueOf(quantity));
            cost = cost.add(taf);
        }
        return cost;
    }
}
