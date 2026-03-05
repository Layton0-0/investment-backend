package com.investment.marketdata.client.impl;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;

/**
 * KoreaInvestmentMarketDataClient 테스트
 *
 * 프로덕션 코드에는 mock 데이터 없음 규칙에 따라,
 * 지표/현재가 조회는 실제 API 또는 테스트에서 WebClient·SecurityContext 모킹 필요.
 * 여기서는 provider 이름 등 기본 동작만 검증.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KoreaInvestmentMarketDataClient 테스트")
class KoreaInvestmentMarketDataClientTest {

    @Mock
    private MarketDataProperties properties;
    @Mock
    private WebClient webClient;
    @Mock
    private RateLimiterRegistry rateLimiterRegistry;
    @Mock
    private KoreaInvestmentTokenService tokenService;
    @Mock
    private UserApiKeyRepository userApiKeyRepository;
    @Mock
    private EncryptionUtil encryptionUtil;

    @InjectMocks
    private KoreaInvestmentMarketDataClient client;

    private MarketDataProperties.KoreaInvestmentProperties kiProps;

    @BeforeEach
    void setUp() {
        kiProps = new MarketDataProperties.KoreaInvestmentProperties();
        kiProps.setAppKey("test-app-key");
        kiProps.setAppSecret("test-app-secret");
        kiProps.setServerType("1");

        lenient().when(properties.getKoreaInvestment()).thenReturn(kiProps);
        lenient().when(properties.getTimeout()).thenReturn(30000);
    }

    @Test
    @DisplayName("Provider 이름 확인")
    void getProviderName() {
        String providerName = client.getProviderName();
        assertEquals("Korea Investment", providerName);
    }
}
