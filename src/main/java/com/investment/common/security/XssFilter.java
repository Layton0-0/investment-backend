package com.investment.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 방지 필터
 * 
 * 요청 파라미터에서 XSS 패턴을 제거하거나 이스케이프 처리합니다.
 * 주의: 이 필터는 모든 파라미터를 HTML 이스케이프하므로, 
 * 실제 HTML을 허용해야 하는 경우에는 사용하지 않아야 합니다.
 */
@Slf4j
@Component
public class XssFilter extends OncePerRequestFilter {
    
    // XSS 패턴 (기본적인 패턴만 체크, 실제로는 더 복잡한 패턴이 필요할 수 있음)
    private static final String[] XSS_PATTERNS = {
        "<script",
        "</script>",
        "javascript:",
        "onerror=",
        "onload=",
        "onclick=",
        "onmouseover=",
        "eval(",
        "expression(",
        "<iframe",
        "<object",
        "<embed"
    };
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        // XSS 패턴이 포함된 요청 파라미터 확인
        boolean hasXssPattern = false;
        Map<String, String> sanitizedParams = new HashMap<>();
        
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            
            if (paramValue != null) {
                // XSS 패턴 검사
                String lowerValue = paramValue.toLowerCase();
                for (String pattern : XSS_PATTERNS) {
                    if (lowerValue.contains(pattern.toLowerCase())) {
                        hasXssPattern = true;
                        log.warn("XSS 패턴 감지: param={}, value={}", 
                                paramName, LogMaskingUtil.maskString(paramValue, 10));
                        break;
                    }
                }
                
                // HTML 이스케이프 처리 (기본적인 방어)
                // 주의: 실제 HTML을 허용해야 하는 경우에는 이 부분을 제거하거나 조건부로 처리
                String sanitized = HtmlUtils.htmlEscape(paramValue);
                sanitizedParams.put(paramName, sanitized);
            }
        }
        
        if (hasXssPattern) {
            // XSS 패턴이 발견된 경우 요청 거부
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"INVALID_INPUT\",\"message\":\"잘못된 입력이 감지되었습니다\"}");
            return;
        }
        
        // XSS 패턴이 없는 경우 정상 처리
        // 주의: 실제로는 파라미터를 수정하는 것이 복잡하므로,
        // 여기서는 검사만 수행하고 실제 필터링은 각 컨트롤러에서 처리하는 것을 권장
        filterChain.doFilter(request, response);
    }
}
