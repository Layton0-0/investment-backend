package com.investment.marketdata.service;

import com.investment.common.security.EncryptionUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.KoreaInvestmentToken;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.KoreaInvestmentTokenRepository;
import com.investment.domain.repository.UserApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 한국투자증권 토큰 관리 서비스
 * 
 * 로그인 시점에 토큰이 DB에 없으면 1회 발급하여 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KoreaInvestmentTokenService {
    
    private final UserApiKeyRepository userApiKeyRepository;
    private final KoreaInvestmentTokenRepository tokenRepository;
    private final EncryptionUtil encryptionUtil;
    private final KoreaInvestmentTokenClient tokenClient;
    
    /**
     * 서버 시작 시 모든 사용자의 토큰 발급
     */
    @Transactional
    public void issueTokensForAllUsers() {
        log.info("한국투자증권 토큰 발급 시작");
        
        List<UserApiKey> userApiKeys = userApiKeyRepository.findAll();
        
        for (UserApiKey userApiKey : userApiKeys) {
            // 한국투자증권만 처리
            if (userApiKey.getBrokerType() == BrokerType.KOREA_INVESTMENT) {
                try {
                    issueTokenForUser(userApiKey);
                } catch (Exception e) {
                    log.error("사용자 토큰 발급 실패: userId={}", userApiKey.getUserId(), e);
                }
            }
        }
        
        log.info("한국투자증권 토큰 발급 완료: 총 {}명", userApiKeys.size());
    }
    
    /**
     * 특정 사용자의 토큰 발급 (새로운 트랜잭션에서 실행)
     * 회원가입 등 다른 트랜잭션과 독립적으로 실행되어, 토큰 발급 실패 시에도
     * 회원가입은 성공하도록 합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueTokenForUserInNewTransaction(UserApiKey userApiKey) {
        issueTokenForUser(userApiKey);
    }
    
    /**
     * 특정 사용자의 토큰 발급
     * 토큰이 유효하면 발급하지 않습니다.
     */
    @Transactional
    public void issueTokenForUser(UserApiKey userApiKey) {
        String userId = userApiKey.getUserId();
        
        // 기존 토큰 확인
        KoreaInvestmentToken existingToken = tokenRepository.findByUserId(userId).orElse(null);
        
        // 토큰이 유효하면 발급하지 않음
        if (existingToken != null && existingToken.isValid()) {
            log.debug("기존 토큰이 유효함: userId={}", userId);
            return;
        }
        
        // API 키 복호화
        String appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
        
        // 토큰 발급
        String accessToken = tokenClient.issueAccessToken(
                appKey, 
                appSecret, 
                userApiKey.getServerType()
        ).block(Duration.ofSeconds(30));
        
        if (accessToken == null) {
            throw new RuntimeException("토큰 발급 실패: userId=" + userId);
        }
        
        // 토큰 암호화하여 저장
        String encryptedToken = encryptionUtil.encrypt(accessToken);
        long expiresAt = System.currentTimeMillis() + (23 * 60 * 60 * 1000); // 23시간
        
        if (existingToken != null) {
            existingToken.updateToken(encryptedToken, expiresAt);
            tokenRepository.save(existingToken);
        } else {
            KoreaInvestmentToken token = KoreaInvestmentToken.builder()
                    .userId(userId)
                    .accessTokenEncrypted(encryptedToken)
                    .expiresAt(expiresAt)
                    .build();
            tokenRepository.save(token);
        }
        
        log.info("토큰 발급 및 저장 완료: userId={}", userId);
    }
    
    /**
     * 로그인 시 토큰이 DB에 없으면 1회 발급
     * 토큰이 이미 존재하면 발급하지 않습니다.
     */
    @Transactional
    public void issueTokenForUserIfNotExists(UserApiKey userApiKey) {
        String userId = userApiKey.getUserId();
        
        // 기존 토큰 확인
        boolean tokenExists = tokenRepository.findByUserId(userId).isPresent();
        
        if (tokenExists) {
            log.debug("토큰이 이미 존재함: userId={}", userId);
            return;
        }
        
        log.info("토큰이 없어서 발급 시작: userId={}", userId);
        issueTokenForUser(userApiKey);
    }
    
    /**
     * 사용자 ID로 토큰 조회 (복호화하여 반환)
     * 토큰이 만료되었으면 자동으로 재발급합니다.
     */
    @Transactional
    public String getAccessToken(String userId) {
        KoreaInvestmentToken token = tokenRepository.findByUserId(userId).orElse(null);
        
        // 토큰이 없거나 만료된 경우 재발급
        if (token == null || !token.isValid()) {
            log.info("토큰이 없거나 만료됨, 재발급 시도: userId={}", userId);
            
            UserApiKey userApiKey = userApiKeyRepository.findByUserIdAndBrokerType(userId, BrokerType.KOREA_INVESTMENT)
                    .orElseThrow(() -> new RuntimeException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));
            
            // 토큰 재발급
            issueTokenForUser(userApiKey);
            
            // 재조회
            token = tokenRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("토큰 발급 후 조회 실패: userId=" + userId));
        }
        
        return encryptionUtil.decrypt(token.getAccessTokenEncrypted());
    }
}
