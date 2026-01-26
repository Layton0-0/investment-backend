package com.investment.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        String traceId = generateTraceId();
        log.warn("DomainException occurred: [{}] {}", e.getErrorCode(), e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getErrorCode())
                .message(e.getMessage())
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e) {
        String traceId = generateTraceId();
        log.error("AppException occurred: [{}] {}", e.getErrorCode(), e.getMessage(), e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(e.getErrorCode())
                .message(e.getMessage())
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String traceId = generateTraceId();
        log.warn("ValidationException occurred: {}", e.getMessage());
        
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT)
                .message("입력값 검증 실패")
                .details(details)
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        String traceId = generateTraceId();
        log.warn("ConstraintViolationException occurred: {}", e.getMessage());
        
        List<String> details = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INVALID_INPUT)
                .message("입력값 검증 실패")
                .details(details)
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        String traceId = generateTraceId();
        log.error("Unexpected exception occurred", e);
        
        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_ERROR)
                .message("시스템 오류가 발생했습니다")
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
