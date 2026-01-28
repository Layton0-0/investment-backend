package com.investment.common.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 요청 유틸리티
 */
public class HttpRequestUtil {
    
    /**
     * 클라이언트 IP 주소 추출
     * 
     * 프록시나 로드 밸런서를 통한 요청도 고려하여 실제 클라이언트 IP를 추출합니다.
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("X-Real-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        
        // X-Forwarded-For는 여러 IP가 콤마로 구분될 수 있음 (첫 번째가 실제 클라이언트)
        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        
        return ipAddress != null ? ipAddress : "unknown";
    }
}
