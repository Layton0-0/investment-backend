package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 마찰 비용(수수료·세금·슬리피지) 설정.
 * 한국투자증권(KIS) 실전 수수료 체계 및 KR/US 시장별 비율.
 * 백테스트 PnL·로보 리밸런싱 비용 반영용.
 */
@Component
@ConfigurationProperties(prefix = "investment.fees")
@Getter
@Setter
public class FrictionCostProperties {

    private KoreaFees korea = new KoreaFees();
    private UsaFees usa = new UsaFees();

    @Getter
    @Setter
    public static class KoreaFees {
        private StockFee stock = new StockFee();
        private EtfFee etf = new EtfFee();
    }

    @Getter
    @Setter
    public static class UsaFees {
        private UsaStockFee stock = new UsaStockFee();
        private CurrencyFee currency = new CurrencyFee();
    }

    /** 한국 주식: 위탁수수료, 증권거래세(매도), 슬리피지 */
    @Getter
    @Setter
    public static class StockFee {
        /** 위탁수수료 (은행제휴 0.0140527%) */
        private BigDecimal commission = new BigDecimal("0.000140527");
        /** 증권거래세 매도 시 (0.18%) */
        private BigDecimal tax = new BigDecimal("0.0018");
        /** 슬리피지 (0.1%) */
        private BigDecimal slippage = new BigDecimal("0.001");
    }

    /** 한국 ETF: 거래세 면제 */
    @Getter
    @Setter
    public static class EtfFee {
        private BigDecimal commission = new BigDecimal("0.000140527");
        private BigDecimal tax = BigDecimal.ZERO;
        private BigDecimal slippage = new BigDecimal("0.0005");
    }

    /** 미국 주식/ETF: 위탁수수료, SEC Fee, TAF, 슬리피지 */
    @Getter
    @Setter
    public static class UsaStockFee {
        /** 위탁수수료 (0.25% 표준) */
        private BigDecimal commission = new BigDecimal("0.0025");
        /** SEC Fee 매도 시 (0.00278%) */
        private BigDecimal secFee = new BigDecimal("0.0000278");
        /** TAF 매도 시 주당 USD (최대 8.30) */
        private BigDecimal tafPerShareUsd = new BigDecimal("0.000166");
        private BigDecimal slippage = new BigDecimal("0.0005");
    }

    /** 미국: 환전 스프레드 (우대 가정) */
    @Getter
    @Setter
    public static class CurrencyFee {
        private BigDecimal exchangeRateSpread = new BigDecimal("0.001");
    }
}
