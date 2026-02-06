package com.investment.common.exception;

/**
 * 에러 코드 상수
 */
public final class ErrorCode {
    
    // 공통
    public static final String INVALID_INPUT = "INVALID_INPUT";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    
    // 계좌 관련
    public static final String ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
    public static final String ACCOUNT_ACCESS_DENIED = "ACCOUNT_ACCESS_DENIED";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    
    // 주문 관련
    public static final String ORDER_FAILED = "ORDER_FAILED";
    public static final String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    public static final String INVALID_ORDER_TYPE = "INVALID_ORDER_TYPE";
    public static final String INVALID_ORDER_AMOUNT = "INVALID_ORDER_AMOUNT";
    public static final String EXCEEDS_MAX_INVESTMENT = "EXCEEDS_MAX_INVESTMENT";
    /** Pre-Trade 컴플라이언스 거부(Kill Switch, 비중 상한, MDD 등) */
    public static final String ORDER_REJECTED = "ORDER_REJECTED";

    // 시장 데이터 API 관련
    public static final String MARKET_DATA_API_ERROR = "MARKET_DATA_API_ERROR";
    public static final String MARKET_DATA_API_TIMEOUT = "MARKET_DATA_API_TIMEOUT";
    public static final String MARKET_DATA_API_CONNECTION_FAILED = "MARKET_DATA_API_CONNECTION_FAILED";
    
    // 설정 관련
    public static final String SETTING_NOT_FOUND = "SETTING_NOT_FOUND";
    public static final String INVALID_SETTING_VALUE = "INVALID_SETTING_VALUE";
    
    // 포트폴리오 관련
    public static final String PORTFOLIO_NOT_FOUND = "PORTFOLIO_NOT_FOUND";
    
    // 인증 관련
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
    public static final String INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String INVALID_BROKER_TYPE = "INVALID_BROKER_TYPE";
    public static final String API_KEY_NOT_FOUND = "API_KEY_NOT_FOUND";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String TOKEN_ISSUANCE_FAILED = "TOKEN_ISSUANCE_FAILED";
    public static final String INVALID_API_CREDENTIALS = "INVALID_API_CREDENTIALS";
    
    private ErrorCode() {
        // 상수 클래스
    }
}
