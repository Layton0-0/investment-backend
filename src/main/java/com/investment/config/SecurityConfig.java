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
import org.springframework.http.HttpMethod;
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
                // 공개 엔드포인트 (읽기 전용 시세/차트는 비인증 허용)
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/market-data/**"
                ).permitAll()
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
                    "/actuator/metrics/**",
                    "/actuator/prometheus",
                    "/api/actuator/health",
                    "/api/actuator/metrics/**",
                    "/api/actuator/prometheus",
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
    
    @Value("${CORS_ALLOWED_ORIGINS:${cors.allowed-origins:*}}")
    private String corsAllowedOrigins;
    
    /**
     * CORS 설정
     * <p>
     * 프론트엔드가 credentials: 'include'(쿠키)를 사용하면 브라우저는
     * Access-Control-Allow-Origin: * 를 허용하지 않습니다. 반드시 구체적인 origin + allowCredentials(true) 필요.
     * </p>
     * <ul>
     *   <li>CORS_ALLOWED_ORIGINS=* (기본): 로컬 개발용으로 http://localhost:5173, http://127.0.0.1:5173 허용 + credentials 허용</li>
     *   <li>CORS_ALLOWED_ORIGINS=url1,url2: 지정한 origin만 허용 + credentials 허용 (프로덕션 권장)</li>
     * </ul>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        String origins = corsAllowedOrigins != null ? corsAllowedOrigins.trim() : "*";

        if ("*".equals(origins)) {
            // 로컬 개발: Vite(5173) 등에서 credentials: 'include' 사용 가능하도록 구체적 origin 허용
            configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
            ));
            configuration.setAllowCredentials(true);
        } else {
            List<String> allowedOrigins = Arrays.stream(origins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            // credentials 사용 시 브라우저는 Allow-Origin: * 를 허용하지 않음. '*' 항목을 개발용 origin으로 치환
            if (allowedOrigins.contains("*")) {
                configuration.setAllowedOrigins(List.of(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173"
                ));
            } else {
                configuration.setAllowedOrigins(allowedOrigins);
            }
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
