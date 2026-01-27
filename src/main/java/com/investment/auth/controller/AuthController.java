package com.investment.auth.controller;

import com.investment.auth.dto.*;
import com.investment.auth.service.AuthService;
import com.investment.common.exception.ErrorCode;
import com.investment.common.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 마이페이지 API")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * 회원가입
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    public ResponseEntity<AuthResponseDto> signup(@Valid @RequestBody SignupRequestDto request) {
        log.info("회원가입 요청: username={}", request.getUsername());
        AuthResponseDto response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급합니다")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        log.info("로그인 요청: username={}", request.getUsername());
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 마이페이지 조회
     */
    @GetMapping("/mypage")
    @Operation(summary = "마이페이지 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
    public ResponseEntity<MyPageResponseDto> getMyPage(Authentication authentication) {
        String userId = authentication.getName();
        log.debug("마이페이지 조회: userId={}", userId);
        MyPageResponseDto response = authService.getMyPage(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 마이페이지 수정
     */
    @PutMapping("/mypage")
    @Operation(summary = "마이페이지 수정", description = "현재 로그인한 사용자의 정보를 수정합니다")
    public ResponseEntity<MyPageResponseDto> updateMyPage(
            Authentication authentication,
            @Valid @RequestBody MyPageUpdateRequestDto request) {
        String userId = authentication.getName();
        log.info("마이페이지 수정 요청: userId={}", userId);
        MyPageResponseDto response = authService.updateMyPage(userId, request);
        return ResponseEntity.ok(response);
    }
}
