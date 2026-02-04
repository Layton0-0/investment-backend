package com.investment.config;

import java.math.BigDecimal;

/**
 * 마찰 비용 기본 비율 상수 (한국투자증권 KIS 보수적 수치).
 * FrictionCostProperties 기본값·문서·테스트 참조용. yml 설정이 우선.
 */
public final class FrictionCostConstants {

    private FrictionCostConstants() {
    }

    /** 한국 주식 위탁수수료 (0.0140527%, 은행제휴) */
    public static final BigDecimal KR_STOCK_COMMISSION = new BigDecimal("0.000140527");
    /** 한국 주식 증권거래세 매도 시 (0.18%) */
    public static final BigDecimal KR_STOCK_TAX = new BigDecimal("0.0018");
    /** 한국 주식 슬리피지 (0.1%) */
    public static final BigDecimal KR_STOCK_SLIPPAGE = new BigDecimal("0.001");

    /** 한국 ETF 위탁수수료 */
    public static final BigDecimal KR_ETF_COMMISSION = new BigDecimal("0.000140527");
    /** 한국 ETF 거래세 (면제) */
    public static final BigDecimal KR_ETF_TAX = BigDecimal.ZERO;
    /** 한국 ETF 슬리피지 (0.05%) */
    public static final BigDecimal KR_ETF_SLIPPAGE = new BigDecimal("0.0005");

    /** 미국 주식/ETF 위탁수수료 (0.25% 표준) */
    public static final BigDecimal USA_STOCK_COMMISSION = new BigDecimal("0.0025");
    /** 미국 SEC Fee 매도 시 (0.00278%) */
    public static final BigDecimal USA_SEC_FEE = new BigDecimal("0.0000278");
    /** 미국 TAF 매도 시 주당 USD */
    public static final BigDecimal USA_TAF_PER_SHARE_USD = new BigDecimal("0.000166");
    /** 미국 슬리피지 (0.05%) */
    public static final BigDecimal USA_STOCK_SLIPPAGE = new BigDecimal("0.0005");

    /** 미국 환전 스프레드 (0.1% 우대 가정) */
    public static final BigDecimal USA_EXCHANGE_RATE_SPREAD = new BigDecimal("0.001");
}
