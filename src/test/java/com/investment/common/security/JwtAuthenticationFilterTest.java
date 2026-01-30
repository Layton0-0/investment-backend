package com.investment.common.security;

import com.investment.auth.service.UserExistenceChecker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter")
class JwtAuthenticationFilterTest {

    private static final String VALID_TOKEN = "valid.jwt.token";
    private static final String USER_ID = "69683173-0000-0000-0000-000000000000";
    private static final String USERNAME = "testuser";

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserExistenceChecker userExistenceChecker;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("토큰 유효 + 사용자 존재 시 인증이 설정된다")
    void whenTokenValidAndUserExists_thenSetsAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(userExistenceChecker.exists(USER_ID)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(USER_ID);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 유효 + 사용자 없음 시 인증을 설정하지 않는다")
    void whenTokenValidAndUserNotExists_thenDoesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(userExistenceChecker.exists(USER_ID)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰 유효 + 사용자 없음 + 쿠키로 전달된 경우 응답에 token 쿠키 제거를 추가한다")
    void whenTokenValidAndUserNotExistsAndFromCookie_thenClearsTokenCookie() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[] { new Cookie("token", VALID_TOKEN) });
        when(request.getHeader("Authorization")).thenReturn(null);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(true);
        when(jwtTokenProvider.getUserId(VALID_TOKEN)).thenReturn(USER_ID);
        when(jwtTokenProvider.getUsername(VALID_TOKEN)).thenReturn(USERNAME);
        when(userExistenceChecker.exists(USER_ID)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response, atLeastOnce()).addCookie(cookieCaptor.capture());
        Collection<Cookie> added = cookieCaptor.getAllValues();
        assertThat(added).anyMatch(c -> "token".equals(c.getName()) && c.getMaxAge() == 0);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("토큰이 없으면 인증을 설정하지 않는다")
    void whenNoToken_thenDoesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtTokenProvider, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("토큰이 무효면 인증을 설정하지 않는다")
    void whenTokenInvalid_thenDoesNotSetAuthentication() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer " + VALID_TOKEN);
        when(jwtTokenProvider.validateToken(VALID_TOKEN)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userExistenceChecker, never()).exists(anyString());
        verify(filterChain).doFilter(request, response);
    }
}
