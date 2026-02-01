package com.investment.account.client;

/**
 * 한국투자증권 계좌 API 상수 정의
 * 
 * TR ID 및 엔드포인트 경로를 정의합니다.
 * 실거래와 모의투자 서버의 TR ID가 다릅니다.
 */
public final class KoreaInvestmentAccountApiConstants {
    
    private KoreaInvestmentAccountApiConstants() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    // Base URL
    public static final String BASE_URL_REAL = "https://openapi.koreainvestment.com:9443";
    public static final String BASE_URL_VIRTUAL = "https://openapivts.koreainvestment.com:29443";
    
    // 엔드포인트 경로
    public static final String PATH_INQUIRE_BALANCE = "/uapi/domestic-stock/v1/trading/inquire-balance";
    /** 해외주식 현재잔고(체결기준) 조회. GET + query. 미국 840, 외화 02. */
    public static final String PATH_OVERSAS_INQUIRE_PRESENT_BALANCE = "/uapi/overseas-stock/v1/trading/inquire-present-balance";
    public static final String PATH_INQUIRE_PSBL_ORDER = "/uapi/domestic-stock/v1/trading/inquire-psbl-order";
    public static final String PATH_INQUIRE_PSBL_ORDER2 = "/uapi/domestic-stock/v1/trading/inquire-psbl-order2";
    public static final String PATH_INQUIRE_PSBL_ORDER3 = "/uapi/domestic-stock/v1/trading/inquire-psbl-order3";
    public static final String PATH_INQUIRE_DAILY_CCLD = "/uapi/domestic-stock/v1/trading/inquire-daily-ccld";
    public static final String PATH_INQUIRE_BALANCE_RLZ_PL = "/uapi/domestic-stock/v1/trading/inquire-balance-rlz-pl";
    public static final String PATH_INQUIRE_ASSETS = "/uapi/domestic-stock/v1/trading/inquire-assets";
    public static final String PATH_INQUIRE_PERIOD_PROFIT_LOSS = "/uapi/domestic-stock/v1/trading/inquire-period-profit-loss";
    
    // 주식잔고조회 TR ID
    public static final String TR_ID_BALANCE_REAL = "TTTC8434R"; // 실거래
    public static final String TR_ID_BALANCE_VIRTUAL = "VTTC8434R"; // 모의투자

    // 해외주식 현재잔고(체결기준) 조회 TR ID
    public static final String TR_ID_OVERSAS_BALANCE_REAL = "CTRP6504R"; // 실거래
    public static final String TR_ID_OVERSAS_BALANCE_VIRTUAL = "VTRP6504R"; // 모의투자
    
    // 매수가능조회 TR ID
    public static final String TR_ID_BUYABLE_REAL = "TTTC8908R"; // 실거래
    public static final String TR_ID_BUYABLE_VIRTUAL = "VTTC8908R"; // 모의투자
    
    // 매도가능수량조회 TR ID
    public static final String TR_ID_SELLABLE_REAL = "TTTC8901R"; // 실거래
    public static final String TR_ID_SELLABLE_VIRTUAL = "VTTC8901R"; // 모의투자
    
    // 주식일별주문체결조회 TR ID (3개월 이내)
    public static final String TR_ID_ORDER_HISTORY_REAL = "TTTC0081R"; // 실거래
    public static final String TR_ID_ORDER_HISTORY_VIRTUAL = "VTTC0081R"; // 모의투자
    
    // 주식일별주문체결조회 TR ID (3개월 이전)
    public static final String TR_ID_ORDER_HISTORY_BEFORE_REAL = "CTSC9215R"; // 실거래
    public static final String TR_ID_ORDER_HISTORY_BEFORE_VIRTUAL = "VTSC9215R"; // 모의투자
    
    // 주식정정취소가능주문조회 TR ID
    public static final String TR_ID_CANCELABLE_ORDER_REAL = "TTTC8002R"; // 실거래
    public static final String TR_ID_CANCELABLE_ORDER_VIRTUAL = "VTTC8002R"; // 모의투자
    
    // 주식잔고조회_실현손익 TR ID
    public static final String TR_ID_BALANCE_RLZ_PL_REAL = "TTTC8494R"; // 실거래
    public static final String TR_ID_BALANCE_RLZ_PL_VIRTUAL = "VTTC8494R"; // 모의투자
    
    // 투자계좌자산현황조회 TR ID
    public static final String TR_ID_ASSETS_REAL = "TTTC8436R"; // 실거래
    public static final String TR_ID_ASSETS_VIRTUAL = "VTTC8436R"; // 모의투자
    
    // 기간별손익일별합산조회 TR ID
    public static final String TR_ID_PERIOD_PROFIT_LOSS_REAL = "TTTC8708R"; // 실거래
    public static final String TR_ID_PERIOD_PROFIT_LOSS_VIRTUAL = "VTTC8708R"; // 모의투자
    
    // 기간별매매손익현황조회 TR ID
    public static final String TR_ID_PERIOD_PROFIT_LOSS_STATUS_REAL = "TTTC8709R"; // 실거래
    public static final String TR_ID_PERIOD_PROFIT_LOSS_STATUS_VIRTUAL = "VTTC8709R"; // 모의투자
    
    /**
     * 서버 타입에 따른 Base URL 반환
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return Base URL
     */
    public static String getBaseUrl(String serverType) {
        if ("0".equals(serverType)) {
            return BASE_URL_REAL;
        }
        return BASE_URL_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (주식잔고조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getBalanceTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_BALANCE_REAL;
        }
        return TR_ID_BALANCE_VIRTUAL;
    }

    /**
     * 서버 타입에 따른 TR ID 반환 (해외주식 현재잔고 조회)
     *
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getOverseasBalanceTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_OVERSAS_BALANCE_REAL;
        }
        return TR_ID_OVERSAS_BALANCE_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (매수가능조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getBuyableTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_BUYABLE_REAL;
        }
        return TR_ID_BUYABLE_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (매도가능수량조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getSellableTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_SELLABLE_REAL;
        }
        return TR_ID_SELLABLE_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (주문체결조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getOrderHistoryTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_ORDER_HISTORY_REAL;
        }
        return TR_ID_ORDER_HISTORY_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (정정취소가능주문조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getCancelableOrderTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_CANCELABLE_ORDER_REAL;
        }
        return TR_ID_CANCELABLE_ORDER_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (실현손익조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getBalanceRlzPlTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_BALANCE_RLZ_PL_REAL;
        }
        return TR_ID_BALANCE_RLZ_PL_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (투자계좌자산현황조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getAssetsTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_ASSETS_REAL;
        }
        return TR_ID_ASSETS_VIRTUAL;
    }
    
    /**
     * 서버 타입에 따른 TR ID 반환 (기간별손익조회)
     * 
     * @param serverType "0": 실거래, "1": 모의투자
     * @return TR ID
     */
    public static String getPeriodProfitLossTrId(String serverType) {
        if ("0".equals(serverType)) {
            return TR_ID_PERIOD_PROFIT_LOSS_REAL;
        }
        return TR_ID_PERIOD_PROFIT_LOSS_VIRTUAL;
    }
}
