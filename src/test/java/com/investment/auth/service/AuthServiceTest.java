package com.investment.auth.service;

import com.investment.auth.dto.MyPageResponseDto;
import com.investment.auth.dto.SignupRequestDto;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.User;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserApiKeyRepository userApiKeyRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.investment.common.security.JwtTokenProvider jwtTokenProvider;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private com.investment.marketdata.service.KoreaInvestmentTokenService tokenService;

    @Mock
    private com.investment.common.security.SecurityAuditService securityAuditService;

    @Mock
    private AccountLockService accountLockService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("signup 사용자명 중복 시 DomainException DUPLICATE_USERNAME")
    void signup_duplicateUsername_throwsDomainException() {
        SignupRequestDto request = new SignupRequestDto();
        request.setUsername("existing");
        request.setPassword("MyP@ssw0rd1");
        request.setBrokerType("KOREA_INVESTMENT");
        request.setAppKey("key");
        request.setAppSecret("secret");
        request.setServerType("1");
        request.setAccountNo("12345678-12");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        DomainException ex = assertThrows(DomainException.class, () -> authService.signup(request));
        assertEquals(ErrorCode.DUPLICATE_USERNAME, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("이미 사용 중"));
    }

    @Test
    @DisplayName("signup 지원하지 않는 증권사 코드 시 DomainException INVALID_BROKER_TYPE")
    void signup_invalidBrokerType_throwsDomainException() {
        SignupRequestDto request = new SignupRequestDto();
        request.setUsername("newuser");
        request.setPassword("MyP@ssw0rd1");
        request.setBrokerType("UNKNOWN_BROKER");
        request.setAppKey("key");
        request.setAppSecret("secret");
        request.setServerType("1");
        request.setAccountNo("12345678-12");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);

        DomainException ex = assertThrows(DomainException.class, () -> authService.signup(request));
        assertEquals(ErrorCode.INVALID_BROKER_TYPE, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("지원하지 않는 증권사"));
    }

    @Test
    @DisplayName("getMyPage 사용자 없을 때 DomainException USER_NOT_FOUND")
    void getMyPage_userNotFound_throwsDomainException() {
        when(userRepository.findById("unknown-id")).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () -> authService.getMyPage("unknown-id"));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("사용자를 찾을 수 없습니다"));
    }

    @Test
    @DisplayName("getMyPage API 키 없을 때 DomainException API_KEY_NOT_FOUND")
    void getMyPage_noApiKey_throwsDomainException() throws Exception {
        User user = User.builder().username("testuser").passwordHash("hash").build();
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, "user-1");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userApiKeyRepository.findByUserId("user-1")).thenReturn(List.of());

        DomainException ex = assertThrows(DomainException.class, () -> authService.getMyPage("user-1"));
        assertEquals(ErrorCode.API_KEY_NOT_FOUND, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("API 키"));
    }

    @Test
    @DisplayName("getMyPage 정상 조회 시 MyPageResponseDto 반환")
    void getMyPage_success_returnsMyPageResponseDto() throws Exception {
        User user = User.builder().username("testuser").passwordHash("hash").build();
        java.lang.reflect.Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, "user-1");
        UserApiKey apiKey = UserApiKey.builder()
                .userId("user-1")
                .brokerType(BrokerType.KOREA_INVESTMENT)
                .appKeyEncrypted("enc-key")
                .appSecretEncrypted("enc-secret")
                .serverType("1")
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userApiKeyRepository.findByUserId("user-1")).thenReturn(List.of(apiKey));
        when(encryptionUtil.decrypt("enc-key")).thenReturn("plain-key");
        when(encryptionUtil.decrypt("enc-secret")).thenReturn("plain-secret");
        when(userAccountRepository.findByUserIdAndServerTypeAndIsDefaultTrue(eq("user-1"), eq("1"))).thenReturn(Optional.empty());

        MyPageResponseDto result = authService.getMyPage("user-1");

        assertNotNull(result);
        assertEquals("user-1", result.getUserId());
        assertEquals("testuser", result.getUsername());
        assertEquals("KOREA_INVESTMENT", result.getBrokerType());
        assertEquals("1", result.getServerType());
    }
}
