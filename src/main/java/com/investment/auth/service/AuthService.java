package com.investment.auth.service;

import com.investment.auth.dto.*;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.JwtTokenProvider;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.User;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.domain.repository.UserRepository;
import com.investment.marketdata.service.KoreaInvestmentTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 인증 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionUtil encryptionUtil;
    private final KoreaInvestmentTokenService tokenService;
    
    /**
     * 회원가입
     */
    @Transactional
    public AuthResponseDto signup(SignupRequestDto request) {
        // 사용자명 중복 확인
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DomainException(ErrorCode.DUPLICATE_USERNAME, "이미 사용 중인 사용자 ID입니다");
        }
        
        // BrokerType 검증
        BrokerType brokerType = BrokerType.fromCode(request.getBrokerType());
        if (brokerType == BrokerType.OTHER && !request.getBrokerType().equals("OTHER")) {
            throw new DomainException(ErrorCode.INVALID_BROKER_TYPE, "지원하지 않는 증권사입니다");
        }
        
        // 사용자 생성
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
        user = userRepository.save(user);
        
        // API 키 암호화하여 저장
        String appKeyEncrypted = encryptionUtil.encrypt(request.getAppKey());
        String appSecretEncrypted = encryptionUtil.encrypt(request.getAppSecret());
        
        UserApiKey userApiKey = UserApiKey.builder()
                .userId(user.getId())
                .brokerType(brokerType)
                .appKeyEncrypted(appKeyEncrypted)
                .appSecretEncrypted(appSecretEncrypted)
                .serverType(request.getServerType())
                .build();
        userApiKeyRepository.save(userApiKey);
        
        log.info("회원가입 완료: userId={}, username={}, brokerType={}", 
                user.getId(), user.getUsername(), brokerType);
        
        // 한국투자증권인 경우 토큰 자동 발급
        // 별도 트랜잭션으로 처리하여 토큰 발급 실패 시에도 회원가입은 성공하도록 함
        if (brokerType == BrokerType.KOREA_INVESTMENT) {
            try {
                // 별도 트랜잭션으로 토큰 발급 시도 (REQUIRES_NEW)
                tokenService.issueTokenForUserInNewTransaction(userApiKey);
                log.info("한국투자증권 토큰 발급 완료: userId={}", user.getId());
            } catch (Exception e) {
                log.error("한국투자증권 토큰 발급 실패: userId={}, error={}", user.getId(), e.getMessage(), e);
                // 토큰 발급 실패해도 회원가입은 성공으로 처리
                // 사용자에게는 경고 메시지로 안내
            }
        }
        
        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        
        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .message("회원가입이 완료되었습니다")
                .build();
    }
    
    /**
     * 로그인
     */
    @Transactional
    public AuthResponseDto login(LoginRequestDto request) {
        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "사용자 ID 또는 비밀번호가 올바르지 않습니다"));
        
        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new DomainException(ErrorCode.INVALID_PASSWORD, "사용자 ID 또는 비밀번호가 올바르지 않습니다");
        }
        
        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);
        
        log.info("로그인 성공: userId={}, username={}", user.getId(), user.getUsername());
        
        // 한국투자증권 사용자인 경우 토큰 체크 및 발급
        List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(user.getId());
        for (UserApiKey userApiKey : userApiKeys) {
            if (userApiKey.getBrokerType() == BrokerType.KOREA_INVESTMENT) {
                try {
                    // 토큰이 DB에 없으면 1회 발급
                    tokenService.issueTokenForUserIfNotExists(userApiKey);
                } catch (Exception e) {
                    log.error("한국투자증권 토큰 발급 실패: userId={}", user.getId(), e);
                    // 토큰 발급 실패해도 로그인은 성공으로 처리
                }
            }
        }
        
        // JWT 토큰 생성
        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        
        return AuthResponseDto.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .message("로그인 성공")
                .build();
    }
    
    /**
     * 마이페이지 조회
     */
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));
        
        List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(userId);
        if (userApiKeys.isEmpty()) {
            throw new DomainException(ErrorCode.API_KEY_NOT_FOUND, "API 키 정보를 찾을 수 없습니다");
        }
        UserApiKey userApiKey = userApiKeys.get(0);
        
        // API Key와 Secret 복호화 후 마스킹
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
        
        return MyPageResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .brokerType(userApiKey.getBrokerType().getCode())
                .brokerTypeName(userApiKey.getBrokerType().getName())
                .appKeyMasked(maskSensitiveData(appKey))
                .appSecretMasked(maskSensitiveData(appSecret))
                .serverType(userApiKey.getServerType())
                .serverTypeName("1".equals(userApiKey.getServerType()) ? "모의투자" : "실거래")
                .build();
    }
    
    /**
     * 마이페이지 수정
     */
    @Transactional
    public MyPageResponseDto updateMyPage(String userId, MyPageUpdateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));
        
        List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(userId);
        if (userApiKeys.isEmpty()) {
            throw new DomainException(ErrorCode.API_KEY_NOT_FOUND, "API 키 정보를 찾을 수 없습니다");
        }
        UserApiKey userApiKey = userApiKeys.get(0);
        
        // 비밀번호 변경
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.updatePassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            log.info("비밀번호 변경: userId={}", userId);
        }
        
        // API 키 정보 변경
        boolean needUpdate = false;
        String appKeyEncrypted = userApiKey.getAppKeyEncrypted();
        String appSecretEncrypted = userApiKey.getAppSecretEncrypted();
        String serverType = userApiKey.getServerType();
        BrokerType brokerType = userApiKey.getBrokerType();
        
        if (request.getAppKey() != null && !request.getAppKey().isEmpty()) {
            appKeyEncrypted = encryptionUtil.encrypt(request.getAppKey());
            needUpdate = true;
        }
        
        if (request.getAppSecret() != null && !request.getAppSecret().isEmpty()) {
            appSecretEncrypted = encryptionUtil.encrypt(request.getAppSecret());
            needUpdate = true;
        }
        
        if (request.getServerType() != null && !request.getServerType().isEmpty()) {
            serverType = request.getServerType();
            needUpdate = true;
        }
        
        if (request.getBrokerType() != null && !request.getBrokerType().isEmpty()) {
            BrokerType newBrokerType = BrokerType.fromCode(request.getBrokerType());
            if (newBrokerType == BrokerType.OTHER && !request.getBrokerType().equals("OTHER")) {
                throw new DomainException(ErrorCode.INVALID_BROKER_TYPE, "지원하지 않는 증권사입니다");
            }
            brokerType = newBrokerType;
            needUpdate = true;
        }
        
        if (needUpdate) {
            userApiKey.updateApiKeys(appKeyEncrypted, appSecretEncrypted, serverType);
            if (request.getBrokerType() != null && !request.getBrokerType().isEmpty()) {
                // BrokerType은 엔티티에 setter가 없으므로 리플렉션 또는 별도 메서드 필요
                // 여기서는 간단하게 새로 생성
                userApiKeyRepository.delete(userApiKey);
                userApiKey = UserApiKey.builder()
                        .userId(userId)
                        .brokerType(brokerType)
                        .appKeyEncrypted(appKeyEncrypted)
                        .appSecretEncrypted(appSecretEncrypted)
                        .serverType(serverType)
                        .build();
                userApiKeyRepository.save(userApiKey);
            } else {
                userApiKeyRepository.save(userApiKey);
            }
            log.info("API 키 정보 변경: userId={}", userId);
        }
        
        // 응답 생성
        String appKey = encryptionUtil.decrypt(appKeyEncrypted);
        String appSecret = encryptionUtil.decrypt(appSecretEncrypted);
        
        return MyPageResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .brokerType(brokerType.getCode())
                .brokerTypeName(brokerType.getName())
                .appKeyMasked(maskSensitiveData(appKey))
                .appSecretMasked(maskSensitiveData(appSecret))
                .serverType(serverType)
                .serverTypeName("1".equals(serverType) ? "모의투자" : "실거래")
                .build();
    }
    
    /**
     * 민감한 데이터 마스킹 (앞 4자리만 표시)
     */
    private String maskSensitiveData(String data) {
        if (data == null || data.length() <= 4) {
            return "****";
        }
        return data.substring(0, 4) + "*".repeat(Math.min(data.length() - 4, 20));
    }
}
