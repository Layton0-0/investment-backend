package com.investment.marketdata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotBlank;

/**
 * 시장 데이터 API 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "investment.market-data")
@Getter
@Setter
public class MarketDataProperties {
    
    /**
     * 사용할 제공자 (finnhub, itick, kiwoom 등)
     */
    private String provider = "finnhub";
    
    /**
     * API 키 (finnhub, itick 등에서 사용)
     */
    private String apiKey;
    
    /**
     * Base URL (REST API 기반 제공자에서 사용)
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
     * 키움증권 API 전용 설정
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
        private String serverType = "1"; // 기본값: 모의투자
        
        /**
         * API 연결 호스트 (로컬에서 실행되는 경우)
         */
        private String host = "localhost";
        
        /**
         * API 연결 포트
         */
        private int port = 5000;
        
        /**
         * 자동 로그인 여부
         */
        private boolean autoLogin = false;
    }
}
