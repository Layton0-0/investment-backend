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
     * 데이터 제공자 (korea-investment)
     */
    private String provider = "korea-investment";
    
    /**
     * 타임아웃 (ms)
     */
    private int timeout = 30000;
    
    /**
     * 모의 데이터 사용 여부
     */
    private boolean useMockData = false;
    
    /**
     * 한국투자증권 API 설정
     */
    private KoreaInvestmentProperties koreaInvestment = new KoreaInvestmentProperties();
    
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
        
        /**
         * 실전투자 여부 확인
         * @return 실전투자면 true, 모의투자면 false
         */
        public boolean isRealTrading() {
            return "0".equals(serverType);
        }
    }
}
