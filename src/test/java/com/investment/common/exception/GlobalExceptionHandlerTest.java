package com.investment.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        ReflectionTestUtils.setField(handler, "debugMode", false);
    }

    @Test
    @DisplayName("DomainException 처리 시 400과 ErrorResponse를 반환한다")
    void handleDomainException_returnsBadRequest() {
        DomainException ex = new DomainException(ErrorCode.DUPLICATE_USERNAME, "이미 사용 중인 사용자 ID입니다");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.DUPLICATE_USERNAME, response.getBody().getCode());
        assertEquals("이미 사용 중인 사용자 ID입니다", response.getBody().getMessage());
        assertNotNull(response.getBody().getTraceId());
    }

    @Test
    @DisplayName("AppException 처리 시 500과 ErrorResponse를 반환한다")
    void handleAppException_returnsInternalError() {
        AppException ex = new AppException(ErrorCode.INTERNAL_ERROR, "내부 오류");

        ResponseEntity<ErrorResponse> response = handler.handleAppException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTERNAL_ERROR, response.getBody().getCode());
        assertEquals("시스템 오류가 발생했습니다", response.getBody().getMessage());
    }

    @Test
    @DisplayName("MethodArgumentNotValidException 처리 시 400과 details를 반환한다")
    void handleValidationException_returnsBadRequestWithDetails() throws Exception {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(
                List.of(new FieldError("signup", "username", "필수값입니다")));
        java.lang.reflect.Method method = GlobalExceptionHandler.class.getDeclaredMethod(
                "handleValidationException", MethodArgumentNotValidException.class);
        org.springframework.core.MethodParameter param = new org.springframework.core.MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INVALID_INPUT, response.getBody().getCode());
        assertEquals("입력값 검증 실패", response.getBody().getMessage());
        assertNotNull(response.getBody().getDetails());
        assertFalse(response.getBody().getDetails().isEmpty());
    }

    @Test
    @DisplayName("NoResourceFoundException 처리 시 404를 반환한다")
    void handleNoResourceFoundException_returnsNotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/favicon.ico");

        ResponseEntity<Void> response = handler.handleNoResourceFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("Exception 처리 시 500과 일반 메시지를 반환한다")
    void handleException_returnsInternalError() {
        Exception ex = new RuntimeException("예기치 않은 오류");

        ResponseEntity<ErrorResponse> response = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(ErrorCode.INTERNAL_ERROR, response.getBody().getCode());
        assertEquals("시스템 오류가 발생했습니다", response.getBody().getMessage());
    }
}
