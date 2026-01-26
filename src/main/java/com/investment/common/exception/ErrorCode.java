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
    
    // 키움 API 관련
    public static final String KIWOOM_API_ERROR = "KIWOOM_API_ERROR";
    public static final String KIWOOM_API_TIMEOUT = "KIWOOM_API_TIMEOUT";
    public static final String KIWOOM_API_CONNECTION_FAILED = "KIWOOM_API_CONNECTION_FAILED";
    
    // 설정 관련
    public static final String SETTING_NOT_FOUND = "SETTING_NOT_FOUND";
    public static final String INVALID_SETTING_VALUE = "INVALID_SETTING_VALUE";
    
    // 포트폴리오 관련
    public static final String PORTFOLIO_NOT_FOUND = "PORTFOLIO_NOT_FOUND";
    
    private ErrorCode() {
        // 상수 클래스
    }
}
