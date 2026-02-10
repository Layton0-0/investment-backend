package com.investment.marketdata.service;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.KoreaInvestmentToken;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.KoreaInvestmentTokenRepository;
import com.investment.domain.repository.UserApiKeyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * KoreaInvestmentTokenService 단위 테스트
 * - 토큰 없을 때 1회만 발급되는지
 * - 기존 토큰 유효 시 발급 API 미호출
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KoreaInvestmentTokenService")
class KoreaInvestmentTokenServiceTest {

    private static final String USER_ID = "user-1";
    private static final String SERVER_TYPE = "1";

    @Mock
    private UserApiKeyRepository userApiKeyRepository;
    @Mock
    private KoreaInvestmentTokenRepository tokenRepository;
    @Mock
    private EncryptionUtil encryptionUtil;
    @Mock
    private KoreaInvestmentTokenClient tokenClient;

    @InjectMocks
    private KoreaInvestmentTokenService tokenService;

    @Test
    @DisplayName("issueTokenForUser - 기존 토큰 유효 시 발급 API 미호출")
    void issueTokenForUser_existingTokenValid_doesNotCallIssueApi() {
        UserApiKey userApiKey = UserApiKey.builder()
                .userId(USER_ID)
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType(SERVER_TYPE)
                .appKeyEncrypted("enc-appKey")
                .appSecretEncrypted("enc-appSecret")
                .build();
        KoreaInvestmentToken existingToken = KoreaInvestmentToken.builder()
                .userId(USER_ID)
                .serverType(SERVER_TYPE)
                .accessTokenEncrypted("enc-token")
                .expiresAt(System.currentTimeMillis() + 3600_000)
                .issuedAt(LocalDateTime.now())
                .build();

        when(tokenRepository.findByUserIdAndServerType(USER_ID, SERVER_TYPE)).thenReturn(Optional.of(existingToken));
        when(encryptionUtil.decrypt("enc-token")).thenReturn("decrypted-token");

        tokenService.issueTokenForUser(userApiKey);

        verify(tokenClient, never()).issueAccessToken(any(), any(), any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("issueTokenForUser - 토큰 없을 때 1회만 발급 API 호출")
    void issueTokenForUser_noToken_callsIssueApiOnce() {
        UserApiKey userApiKey = UserApiKey.builder()
                .userId(USER_ID)
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .serverType(SERVER_TYPE)
                .appKeyEncrypted("enc-appKey")
                .appSecretEncrypted("enc-appSecret")
                .build();

        when(tokenRepository.findByUserIdAndServerType(USER_ID, SERVER_TYPE)).thenReturn(Optional.empty());
        when(encryptionUtil.decrypt("enc-appKey")).thenReturn("appKey");
        when(encryptionUtil.decrypt("enc-appSecret")).thenReturn("appSecret");
        when(encryptionUtil.encrypt("new-access-token")).thenReturn("enc-token");
        when(tokenClient.issueAccessToken(eq("appKey"), eq("appSecret"), eq(SERVER_TYPE)))
                .thenReturn(Mono.just("new-access-token"));

        tokenService.issueTokenForUser(userApiKey);

        verify(tokenClient, times(1)).issueAccessToken("appKey", "appSecret", SERVER_TYPE);
        verify(tokenRepository, times(1)).save(argThat(token ->
                USER_ID.equals(token.getUserId()) && SERVER_TYPE.equals(token.getServerType())));
    }
}
