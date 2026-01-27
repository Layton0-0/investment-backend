package com.investment.marketdata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 시장 데이터 API 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "investment.market-data")
@Getter
@Setter
public class MarketDataProperties {
    
    /**
     * 데이터 제공자 (finnhub, itick, kiwoom, korea-investment 등)
     */
    private String provider = "finnhub";
    
    /**
     * API 키 (finnhub, itick 등에서 사용)
     */
    private String apiKey;
    
    /**
     * Base URL (REST API 서버 주소 등)
     */
    private String baseUrl;
    
    /**
     * 타임아웃 (ms)
     */
    private int timeout = 30000;
    
    /**
     * 모의 데이터 사용 여부
     */
    private boolean useMockData = false;
    
    /**
     * 키움증권 API 설정
     */
    private KiwoomProperties kiwoom = new KiwoomProperties();
    
    /**
     * 한국투자증권 API 설정
     */
    private KoreaInvestmentProperties koreaInvestment = new KoreaInvestmentProperties();
    
    /**
     * 키움증권 API 설정
     */
    @Getter
    @Setter
    public static class KiwoomProperties {
        /**
         * 계좌번호
         */
        private String accountNo;
        
        /**
         * 계좌 비밀번호
         */
        private String password;
        
        /**
         * 서버 타입 (모의투자: "1", 실거래: "0")
         */
        private String serverType = "1"; // 기본: 모의투자
        
        /**
         * API 서버 주소 (별도 서버 구축 시)
         */
        private String host = "localhost";
        
        /**
         * API 서버 포트
         */
        private int port = 5000;
        
        /**
         * 자동 로그인 여부
         */
        private boolean autoLogin = false;
    }
    
    /**
     * 한국투자증권 API 설정
     */
    @Getter
    @Setter
    public static class KoreaInvestmentProperties {
        /**
         * App Key (한국투자증권 API 키)
         */
        private String appKey;
        
        /**
         * App Secret (한국투자증권 API 시크릿)
         */
        private String appSecret;
        
        /**
         * 서버 타입 (모의투자: "1", 실거래: "0")
         */
        private String serverType = "1"; // 기본: 모의투자
        
        /**
         * Access Token 캐시 사용 여부
         */
        private boolean useTokenCache = true;
    }
}
