package com.investment.common.exception;

/**
 * 도메인 비즈니스 예외
 * 비즈니스 규칙 위반 시 사용
 */
public class DomainException extends RuntimeException {
    
    private final String errorCode;
    
    public DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DomainException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
