package com.investment.common.security;

import com.investment.auth.service.UserExistenceChecker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 *
 * 요청 헤더 또는 쿠키에서 JWT 토큰을 추출하여 인증 정보를 설정합니다.
 * 토큰이 유효해도 DB에 해당 사용자가 없으면 인증을 설정하지 않습니다 (방어코드).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_COOKIE_NAME = "token";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserExistenceChecker userExistenceChecker;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            TokenSource tokenSource = extractToken(request);

            if (tokenSource != null && jwtTokenProvider.validateToken(tokenSource.token)) {
                String userId = jwtTokenProvider.getUserId(tokenSource.token);
                String username = jwtTokenProvider.getUsername(tokenSource.token);

                var userOpt = userExistenceChecker.findUser(userId);
                if (userOpt.isEmpty()) {
                    log.warn("JWT는 유효하나 DB에 사용자가 없음: userId={}, username={}",
                            LogMaskingUtil.maskUserId(userId),
                            LogMaskingUtil.maskUsername(username));
                    if (log.isDebugEnabled()) {
                        log.debug("  [DEBUG] userId(actual)={}, username(actual)={}", userId, username);
                    }
                    if (tokenSource.fromCookie) {
                        clearTokenCookie(response);
                    }
                    // SecurityContext 미설정 → 비인증 처리 (401 또는 /login 리다이렉트)
                } else {
                    var user = userOpt.get();
                    List<GrantedAuthority> authorities = Role.fromDbRole(user.getRole());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("JWT 인증 성공: userId={}, username={}, role={}",
                            LogMaskingUtil.maskUserId(userId),
                            LogMaskingUtil.maskUsername(username),
                            user.getRole());
                    if (log.isDebugEnabled()) {
                        log.debug("  [DEBUG] userId(actual)={}, username(actual)={}", userId, username);
                    }
                }
            }
        } catch (Exception e) {
            log.error("JWT 인증 필터 오류", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 응답에 token 쿠키 제거 헤더를 추가하여 클라이언트가 무효 토큰을 재전송하지 않도록 한다.
     */
    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);
    }

    /**
     * 요청에서 JWT 토큰 추출
     * 1. Authorization 헤더: Bearer &lt;token&gt; 형식 (API 요청용)
     * 2. 쿠키: token (웹 페이지 요청용)
     *
     * @return 토큰과 출처(쿠키 여부). 없으면 null
     */
    private TokenSource extractToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 추출 (API 요청용)
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return new TokenSource(bearerToken.substring(7), false);
        }

        // 2. 쿠키에서 토큰 추출 (웹 페이지 요청용)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        return new TokenSource(token, true);
                    }
                }
            }
        }

        return null;
    }

    /**
     * 추출된 JWT 토큰과 출처(쿠키 여부).
     */
    private static final class TokenSource {
        final String token;
        final boolean fromCookie;

        TokenSource(String token, boolean fromCookie) {
            this.token = token;
            this.fromCookie = fromCookie;
        }
    }
}
