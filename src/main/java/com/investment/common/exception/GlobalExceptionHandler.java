package com.investment.common.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 
 * 프로덕션 환경에서는 상세한 에러 정보를 클라이언트에 노출하지 않습니다.
 * 상세 정보는 내부 로그에만 기록됩니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${DEBUG_MODE:false}")
    private boolean debugMode;

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException e) {
        String traceId = generateTraceId();
        // 상세 정보는 내부 로그에만 기록
        if (debugMode) {
            log.warn("DomainException occurred: [{}] {}", e.getErrorCode(), e.getMessage(), e);
        } else {
            log.warn("DomainException occurred: [{}] {} [traceId={}]",
                    e.getErrorCode(), e.getMessage(), traceId);
        }

        ErrorResponse response = ErrorResponse.builder()
                .code(e.getErrorCode())
                .message(e.getMessage()) // 비즈니스 예외는 메시지 노출 허용
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        HttpStatus status = ErrorCode.ACCOUNT_NOT_FOUND.equals(e.getErrorCode())
                ? HttpStatus.NOT_FOUND
                : ErrorCode.SETTING_NOT_FOUND.equals(e.getErrorCode())
                ? HttpStatus.NOT_FOUND
                : ErrorCode.API_NOT_SUPPORTED.equals(e.getErrorCode())
                ? HttpStatus.BAD_REQUEST
                : ErrorCode.UNAUTHORIZED.equals(e.getErrorCode())
                ? HttpStatus.UNAUTHORIZED
                : ErrorCode.ORDER_REJECTED.equals(e.getErrorCode())
                ? HttpStatus.FORBIDDEN
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e) {
        String traceId = generateTraceId();
        // 상세 정보는 내부 로그에만 기록
        if (debugMode) {
            log.error("AppException occurred: [{}] {}", e.getErrorCode(), e.getMessage(), e);
        } else {
            log.error("AppException occurred: [{}] {} [traceId={}]",
                    e.getErrorCode(), e.getMessage(), traceId, e);
        }

        // 프로덕션에서는 일반적인 메시지만 반환
        String userMessage = debugMode ? e.getMessage() : "시스템 오류가 발생했습니다";

        ErrorResponse response = ErrorResponse.builder()
                .code(e.getErrorCode())
                .message(userMessage)
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

    /**
     * 정적 리소스(favicon.ico 등)를 찾을 수 없을 때 발생하는 예외 처리
     * 브라우저/DevTools가 자동으로 요청하는 리소스는 로깅 생략
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        String path = e.getResourcePath();
        if (path == null) {
            return ResponseEntity.notFound().build();
        }
        // 브라우저/도구가 자동 요청하는 경로는 로깅하지 않음 (로그 노이즈 감소)
        boolean skipLogging = path.contains("favicon")
                || path.contains(".well-known")
                || path.contains("com.chrome.devtools");
        if (!skipLogging) {
            log.debug("Resource not found: {}", path);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 트랜잭션 롤백 예외 처리
     * 내부 트랜잭션에서 예외가 발생하여 전체 트랜잭션이 롤백된 경우
     */
    @ExceptionHandler(org.springframework.transaction.UnexpectedRollbackException.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedRollbackException(
            org.springframework.transaction.UnexpectedRollbackException e) {
        String traceId = generateTraceId();
        log.error("Transaction rollback occurred", e);

        // 원인 예외 확인
        Throwable cause = e.getCause();
        String message = "처리 중 오류가 발생했습니다";

        // 원인 예외의 메시지 추출
        if (cause != null) {
            String causeMessage = cause.getMessage();
            if (causeMessage != null && !causeMessage.isEmpty()) {
                // 암호화 키 관련 에러인 경우 사용자 친화적인 메시지 제공
                if (causeMessage.contains("복호화 실패") || causeMessage.contains("암호화 키")) {
                    message = "API 키 복호화에 실패했습니다. 마이페이지에서 API 키를 다시 입력해주세요.";
                } else if (causeMessage.contains("API 키 복호화 실패")) {
                    message = causeMessage;
                } else {
                    message = causeMessage;
                }
            }
        }

        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_ERROR)
                .message(message)
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        String traceId = generateTraceId();
        // 상세 정보는 내부 로그에만 기록
        if (debugMode) {
            log.error("Unexpected exception occurred [traceId={}]", traceId, e);
        } else {
            // 프로덕션에서는 스택 트레이스 없이 로깅
            log.error("Unexpected exception occurred: {} [traceId={}]",
                    e.getClass().getSimpleName() + ": " + e.getMessage(), traceId);
        }

        // 프로덕션에서는 일반적인 메시지만 반환
        String userMessage = debugMode ? (e.getMessage() != null ? e.getMessage() : "시스템 오류가 발생했습니다")
                : "시스템 오류가 발생했습니다";

        ErrorResponse response = ErrorResponse.builder()
                .code(ErrorCode.INTERNAL_ERROR)
                .message(userMessage)
                .traceId(traceId)
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString();
    }
}
