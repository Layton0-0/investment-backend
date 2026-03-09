package com.investment.auth.controller;

import com.investment.auth.dto.*;
import com.investment.auth.service.AccountVerificationService;
import com.investment.auth.service.AuthService;
import com.investment.common.security.HttpRequestUtil;
import com.investment.common.security.LogMaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
    private final AccountVerificationService accountVerificationService;

    @Value("${investment.security.jwt-expiration:3600000}")
    private long jwtExpiration;

    @Value("${COOKIE_SECURE:true}")
    private boolean cookieSecure;

    @Value("${COOKIE_SAME_SITE:Strict}")
    private String cookieSameSite;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    public ResponseEntity<AuthResponseDto> signup(
            @Valid @RequestBody SignupRequestDto request,
            HttpServletResponse httpResponse) {
        LogMaskingUtil.logWithDebugActual(log, "회원가입 요청: username={}",
                new Object[] { LogMaskingUtil.maskUsername(request.getUsername()) },
                "username(actual)={}", request.getUsername());
        AuthResponseDto response = authService.signup(request);

        // 보안 강화된 쿠키 설정
        setSecureCookie(httpResponse, "token", response.getToken(), (int) (jwtExpiration / 1000));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 계좌인증 (회원가입 전): API Key/Secret·서버타입·계좌번호가 해당 도메인(모의 29443 / 실전 9443)에서 유효한지
     * 확인.
     */
    @PostMapping("/verify-account")
    @Operation(summary = "계좌인증", description = "회원가입 전 API Key·계좌번호가 선택한 서버(모의/실거래)에서 유효한지 확인합니다")
    public ResponseEntity<AccountVerifyResponseDto> verifyAccount(
            @Valid @RequestBody AccountVerifyRequestDto request) {
        AccountVerifyResponseDto response = accountVerificationService.verifyAccount(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 인증 후 JWT 토큰을 발급합니다")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LogMaskingUtil.logWithDebugActual(log, "로그인 요청: username={}",
                new Object[] { LogMaskingUtil.maskUsername(request.getUsername()) },
                "username(actual)={}", request.getUsername());
        String ipAddress = HttpRequestUtil.getClientIpAddress(httpRequest);
        AuthResponseDto response = authService.login(request, ipAddress);

        // 보안 강화된 쿠키 설정
        setSecureCookie(httpResponse, "token", response.getToken(), (int) (jwtExpiration / 1000));

        return ResponseEntity.ok(response);
    }

    /**
     * 마이페이지 조회
     */
    @GetMapping("/mypage")
    @Operation(summary = "마이페이지 조회", description = "현재 로그인한 사용자의 정보를 조회합니다")
    public ResponseEntity<MyPageResponseDto> getMyPage(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = authentication.getName();
        log.info("마이페이지 조회: userId={}", LogMaskingUtil.maskUserId(userId));
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
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId = authentication.getName();
        log.info("마이페이지 수정 요청: userId={}", LogMaskingUtil.maskUserId(userId));
        MyPageResponseDto response = authService.updateMyPage(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "사용자 로그아웃 처리")
    public ResponseEntity<Void> logout(HttpServletResponse httpResponse) {
        log.info("로그아웃 요청");

        // 쿠키 삭제 (MaxAge를 0으로 설정)
        setSecureCookie(httpResponse, "token", "", 0);

        return ResponseEntity.ok().build();
    }

    /**
     * 보안 강화된 쿠키 설정
     * - HttpOnly: JavaScript 접근 방지
     * - Secure: HTTPS 환경에서만 전송 (환경 변수로 제어)
     * - SameSite: CSRF 공격 방지 (환경 변수로 제어)
     */
    private void setSecureCookie(HttpServletResponse response, String name, String value, int maxAge) {
        // 기본 Cookie 객체 생성
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);

        // SameSite 속성은 Cookie 클래스에서 직접 지원하지 않으므로
        // Set-Cookie 헤더에 직접 추가
        String cookieHeader = String.format("%s=%s; Path=/; HttpOnly; %s; %s",
                name,
                value.isEmpty() ? "" : value,
                cookieSecure ? "Secure" : "",
                "SameSite=" + cookieSameSite);

        // 기존 Set-Cookie 헤더가 있으면 추가, 없으면 새로 설정
        response.setHeader(HttpHeaders.SET_COOKIE, cookieHeader);
    }
}
