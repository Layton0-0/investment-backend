package com.investment.marketdata.client.impl;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.marketdata.config.MarketDataProperties;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KoreaInvestmentRankClientImpl")
class KoreaInvestmentRankClientImplTest {

    private static final String USER_ID = "user-1";
    private static final String SERVER_TYPE = "1";

    @Mock
    private MarketDataProperties properties;
    @Mock
    private MarketDataProperties.KoreaInvestmentProperties koreaInvestment;
    @Mock
    private MarketDataProperties.KoreaInvestmentProperties.RankApiProperties rankApi;
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

    private KoreaInvestmentRankClientImpl client;

    @BeforeEach
    void setUp() {
        when(properties.getKoreaInvestment()).thenReturn(koreaInvestment);
        when(koreaInvestment.getRankApi()).thenReturn(rankApi);
        client = new KoreaInvestmentRankClientImpl(
                properties, webClient, rateLimiterRegistry,
                tokenService, userApiKeyRepository, encryptionUtil);
    }

    @Test
    @DisplayName("getVolumeRank - path/trId 미설정 시 빈 리스트 반환")
    void getVolumeRank_returnsEmptyWhenNotConfigured() {
        when(rankApi.getVolumeRankPath()).thenReturn("");
        when(rankApi.getVolumeRankTrId()).thenReturn("");

        List<?> result = client.getVolumeRank(USER_ID, SERVER_TYPE, "J", 20);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getInvestorDailyByMarket - path/trId 미설정 시 빈 리스트 반환")
    void getInvestorDailyByMarket_returnsEmptyWhenNotConfigured() {
        when(rankApi.getInvestorDailyPath()).thenReturn("");
        when(rankApi.getInvestorDailyTrId()).thenReturn("");

        List<?> result = client.getInvestorDailyByMarket(
                USER_ID, SERVER_TYPE, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(result).isEmpty();
    }
}
