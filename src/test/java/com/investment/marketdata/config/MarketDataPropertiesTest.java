package com.investment.marketdata.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MarketDataProperties 테스트
 */
@SpringBootTest(classes = MarketDataProperties.class)
@EnableConfigurationProperties(MarketDataProperties.class)
@TestPropertySource(properties = {
        "investment.market-data.provider=korea-investment",
        "investment.market-data.timeout=30000",
        "investment.market-data.korea-investment.app-key=test-app-key",
        "investment.market-data.korea-investment.app-secret=test-app-secret",
        "investment.market-data.korea-investment.server-type=1",
        "investment.market-data.korea-investment.use-token-cache=true"
})
@DisplayName("MarketDataProperties 테스트")
class MarketDataPropertiesTest {
    
    private MarketDataProperties properties;
    
    @BeforeEach
    void setUp() {
        properties = new MarketDataProperties();
    }
    
    @Test
    @DisplayName("기본 provider 값 확인")
    void 기본_provider_값() {
        // when
        String provider = properties.getProvider();
        
        // then
        assertEquals("korea-investment", provider);
    }
    
    @Test
    @DisplayName("provider 설정")
    void provider_설정() {
        // given
        String expectedProvider = "korea-investment";
        
        // when
        properties.setProvider(expectedProvider);
        
        // then
        assertEquals(expectedProvider, properties.getProvider());
    }
    
    @Test
    @DisplayName("기본 timeout 값 확인")
    void 기본_timeout_값() {
        // when
        int timeout = properties.getTimeout();
        
        // then
        assertEquals(30000, timeout);
    }
    
    @Test
    @DisplayName("timeout 설정")
    void timeout_설정() {
        // given
        int expectedTimeout = 60000;
        
        // when
        properties.setTimeout(expectedTimeout);
        
        // then
        assertEquals(expectedTimeout, properties.getTimeout());
    }
    
    @Test
    @DisplayName("한국투자증권 설정 기본값 확인")
    void 한국투자증권_설정_기본값() {
        // when
        MarketDataProperties.KoreaInvestmentProperties kiProps = properties.getKoreaInvestment();
        
        // then
        assertNotNull(kiProps);
        assertEquals("1", kiProps.getServerType());
        assertTrue(kiProps.isUseTokenCache());
    }
    
    @Test
    @DisplayName("한국투자증권 App Key 설정")
    void 한국투자증권_AppKey_설정() {
        // given
        String appKey = "test-app-key";
        MarketDataProperties.KoreaInvestmentProperties kiProps = properties.getKoreaInvestment();
        
        // when
        kiProps.setAppKey(appKey);
        
        // then
        assertEquals(appKey, kiProps.getAppKey());
    }
    
    @Test
    @DisplayName("한국투자증권 App Secret 설정")
    void 한국투자증권_AppSecret_설정() {
        // given
        String appSecret = "test-app-secret";
        MarketDataProperties.KoreaInvestmentProperties kiProps = properties.getKoreaInvestment();
        
        // when
        kiProps.setAppSecret(appSecret);
        
        // then
        assertEquals(appSecret, kiProps.getAppSecret());
    }
    
    @Test
    @DisplayName("한국투자증권 서버 타입 설정")
    void 한국투자증권_서버타입_설정() {
        // given
        String serverType = "0"; // 실거래
        MarketDataProperties.KoreaInvestmentProperties kiProps = properties.getKoreaInvestment();
        
        // when
        kiProps.setServerType(serverType);
        
        // then
        assertEquals(serverType, kiProps.getServerType());
    }
    
    @Test
    @DisplayName("한국투자증권 토큰 캐시 설정")
    void 한국투자증권_토큰캐시_설정() {
        // given
        MarketDataProperties.KoreaInvestmentProperties kiProps = properties.getKoreaInvestment();
        
        // when
        kiProps.setUseTokenCache(false);
        
        // then
        assertFalse(kiProps.isUseTokenCache());
    }
}
