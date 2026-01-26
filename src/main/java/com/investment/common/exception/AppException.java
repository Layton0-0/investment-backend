package com.investment.common.exception;

/**
 * 애플리케이션 예외
 * 시스템 레벨 오류 시 사용
 */
public class AppException extends RuntimeException {
    
    private final String errorCode;
    
    public AppException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AppException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
