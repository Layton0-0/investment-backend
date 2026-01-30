package com.investment.order.client;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import com.investment.marketdata.util.KoreaInvestmentHashkeyUtil;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 한국투자증권 주문 API 클라이언트 테스트
 */
@ExtendWith(MockitoExtension.class)
class KoreaInvestmentOrderClientTest {

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

    @Mock
    private Environment environment;

    @Mock
    private KoreaInvestmentHashkeyUtil hashkeyUtil;

    private KoreaInvestmentOrderClient orderClient;

    @BeforeEach
    void setUp() {
        orderClient = new KoreaInvestmentOrderClient(
                webClient,
                rateLimiterRegistry,
                tokenService,
                userApiKeyRepository,
                encryptionUtil,
                environment,
                hashkeyUtil);
    }

    @Test
    void 주문_클라이언트_생성_성공() {
        assertNotNull(orderClient);
    }

    // 실제 API 호출 테스트는 통합 테스트에서 수행
    // 단위 테스트에서는 Mock을 사용하여 클라이언트 구조만 검증
}
