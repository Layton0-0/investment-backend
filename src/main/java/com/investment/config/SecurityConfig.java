package com.investment.config;

import com.investment.common.security.JwtAuthenticationFilter;
import com.investment.common.security.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 메서드 레벨 보안 활성화
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityHeadersConfig securityHeadersConfig;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(AbstractHttpConfigurer::disable)
            
            // CORS 설정
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 세션 사용 안 함 (JWT 사용)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 요청 인가 설정
            .authorizeHttpRequests(auth -> auth
                // 공개 엔드포인트
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/api/v1/public/**",
                    "/api/v1/internal/**",
                    "/login",
                    "/register",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/actuator/health",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-resources/**"
                ).permitAll()
                
                // 나머지 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // 인증 실패 시 처리
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response,
                                       AuthenticationException authException) throws IOException {
                        // API 요청인 경우 401 반환
                        if (request.getRequestURI().startsWith("/api/")) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"error\":\"인증이 필요합니다\"}");
                        } else {
                            // 웹 페이지 요청인 경우 /login으로 리다이렉트
                            response.sendRedirect("/login");
                        }
                    }
                })
            )
            
            // 필터를 역순으로 추가 (각 필터를 이미 등록된 필터 앞에 추가)
            // 1. JWT 필터 추가 (UsernamePasswordAuthenticationFilter 앞)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 2. Rate Limiting 필터 추가 (JWT 필터 앞 - 이미 등록됨)
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class)
            
            // 3. 보안 헤더 필터 추가 (Rate Limiting 필터 앞 - 이미 등록됨, 가장 먼저 실행)
            .addFilterBefore(securityHeadersConfig, RateLimitFilter.class);
        
        return http.build();
    }
    
    @Value("${CORS_ALLOWED_ORIGINS:*}")
    private String corsAllowedOrigins;
    
    /**
     * CORS 설정
     * 
     * 프로덕션에서는 환경 변수로 특정 도메인만 허용하도록 설정
     * 개발/로컬 환경에서는 모든 origin 허용 가능
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 Origin 설정
        if ("*".equals(corsAllowedOrigins)) {
            // 개발 환경: 모든 origin 허용
            configuration.setAllowedOrigins(List.of("*"));
            configuration.setAllowCredentials(false); // allowCredentials와 *는 함께 사용 불가
        } else {
            // 프로덕션 환경: 특정 도메인만 허용
            List<String> allowedOrigins = Arrays.asList(corsAllowedOrigins.split(","));
            configuration.setAllowedOrigins(allowedOrigins.stream()
                    .map(String::trim)
                    .toList());
            configuration.setAllowCredentials(true);
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L); // 1시간
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
