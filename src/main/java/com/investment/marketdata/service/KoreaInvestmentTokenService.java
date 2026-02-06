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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 한국투자증권 토큰 관리 서비스
 *
 * 로그인 시점에 토큰이 DB에 없으면 1회 발급하여 저장합니다.
 * 한국투자증권 API 접근토큰 발급 1분당 1회 제한을 준수하며,
 * 동시 발급은 사용자 단위 락으로 직렬화합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KoreaInvestmentTokenService {

    private final UserApiKeyRepository userApiKeyRepository;
    private final KoreaInvestmentTokenRepository tokenRepository;
    private final EncryptionUtil encryptionUtil;
    private final KoreaInvestmentTokenClient tokenClient;

    // 최근 토큰 발급 이력 추적 (userId|serverType -> 발급 시각(밀리초))
    // 최근 5초 내 발급 이력이 있으면 재발급을 방지하기 위함
    private final ConcurrentHashMap<String, Long> recentTokenIssuance = new ConcurrentHashMap<>();
    private static final long RECENT_ISSUANCE_WINDOW_MS = 5000; // 5초

    /** 한국투자증권 API 제한: 접근토큰 발급 1분당 1회 (사용자당) */
    private static final long TOKEN_ISSUANCE_COOLDOWN_MS = 60_000L;

    /** 사용자별 마지막 토큰 발급 시각(밀리초). 사용자당 1분 1회 제한용 */
    private final ConcurrentHashMap<String, Long> lastIssuanceTimeByUserId = new ConcurrentHashMap<>();

    /** 사용자 단위 발급 락. 동시에 모의/실 두 타입 발급이 겹치지 않도록 직렬화 */
    private final ConcurrentHashMap<String, Object> issuanceLockByUserId = new ConcurrentHashMap<>();

    private static String issuanceKey(String userId, String serverType) {
        return userId + "|" + (serverType != null ? serverType : "1");
    }

    private Object lockForUser(String userId) {
        return issuanceLockByUserId.computeIfAbsent(userId, k -> new Object());
    }

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
     * 
     * @param userApiKey 사용자 API 키
     * @return 발급된 Access Token (복호화된 상태)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String issueTokenForUserInNewTransaction(UserApiKey userApiKey) {
        issueTokenForUser(userApiKey);

        // 발급한 토큰을 복호화하여 반환
        String userId = userApiKey.getUserId();
        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";
        KoreaInvestmentToken token = tokenRepository.findByUserIdAndServerType(userId, serverType)
                .orElseThrow(() -> new RuntimeException("토큰 발급 후 조회 실패: userId=" + userId + ", serverType=" + serverType));

        return encryptionUtil.decrypt(token.getAccessTokenEncrypted());
    }

    /**
     * 계좌인증 시 미리 발급받은 접근 토큰을 저장합니다.
     * 회원가입 시 재발급 없이 이 토큰을 DB에 저장할 때 사용합니다.
     *
     * @param userId      사용자 ID (회원가입 직후의 user.getId())
     * @param accessToken 계좌인증 시 발급받은 한국투자증권 접근 토큰 (평문)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePreIssuedTokenForUser(String userId, String accessToken) {
        savePreIssuedTokenForUser(userId, "1", accessToken);
    }

    /**
     * 계좌인증 시 미리 발급받은 접근 토큰을 저장합니다 (서버 타입 지정).
     *
     * @param userId      사용자 ID
     * @param serverType  서버 타입 ("1": 모의투자, "0": 실거래)
     * @param accessToken 계좌인증 시 발급받은 한국투자증권 접근 토큰 (평문)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePreIssuedTokenForUser(String userId, String serverType, String accessToken) {
        if (userId == null || accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("userId와 accessToken은 필수입니다.");
        }
        String st = serverType != null ? serverType : "1";
        String encryptedToken = encryptionUtil.encrypt(accessToken.trim());
        long expiresAt = System.currentTimeMillis() + (23 * 60 * 60 * 1000); // 23시간

        KoreaInvestmentToken existingToken = tokenRepository.findByUserIdAndServerType(userId, st).orElse(null);
        if (existingToken != null) {
            existingToken.updateToken(encryptedToken, expiresAt);
            tokenRepository.save(existingToken);
        } else {
            KoreaInvestmentToken token = KoreaInvestmentToken.builder()
                    .userId(userId)
                    .serverType(st)
                    .accessTokenEncrypted(encryptedToken)
                    .expiresAt(expiresAt)
                    .issuedAt(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);
        }
        recentTokenIssuance.put(issuanceKey(userId, st), System.currentTimeMillis());
        log.info("선발급 토큰 저장 완료: userId={}, serverType={}", userId, st);
    }

    /**
     * 특정 사용자의 토큰 발급
     * 토큰이 유효하면 발급하지 않습니다.
     */
    @Transactional
    public void issueTokenForUser(UserApiKey userApiKey) {
        String userId = userApiKey.getUserId();
        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";

        // 기존 토큰 확인 (서버 타입별)
        KoreaInvestmentToken existingToken = tokenRepository.findByUserIdAndServerType(userId, serverType).orElse(null);

        // 토큰이 유효하면 발급하지 않음
        // 단, 복호화 가능 여부도 확인 (암호화 키 변경 시 대비)
        if (existingToken != null && existingToken.isValid()) {
            // 복호화 가능 여부 확인
            try {
                encryptionUtil.decrypt(existingToken.getAccessTokenEncrypted());
                log.debug("기존 토큰이 유효함: userId={}", userId);
                return;
            } catch (RuntimeException e) {
                // 복호화 실패 시 토큰 삭제 후 재발급
                log.warn("기존 토큰 복호화 실패, 삭제 후 재발급: userId={}, error={}", userId, e.getMessage());
                tokenRepository.delete(existingToken);
                existingToken = null;
            }
        }

        // API 키 복호화
        String appKey;
        String appSecret;
        try {
            appKey = encryptionUtil.decrypt(userApiKey.getAppKeyEncrypted());
            appSecret = encryptionUtil.decrypt(userApiKey.getAppSecretEncrypted());
        } catch (RuntimeException e) {
            log.error("API 키 복호화 실패: userId={}, error={}", userId, e.getMessage());
            throw new RuntimeException(
                    String.format("API 키 복호화 실패. 암호화 키가 변경되었거나 데이터가 손상되었을 수 있습니다. " +
                            "마이페이지에서 API 키를 다시 입력해주세요. (userId: %s)", userId),
                    e);
        }

        // 토큰 발급 (비동기 처리)
        // 주의: 트랜잭션 내에서 block() 사용 시 데드락 위험이 있으므로,
        // 별도 트랜잭션에서 실행되거나 비동기 처리 후 결과를 기다려야 함
        // 현재는 REQUIRES_NEW로 별도 트랜잭션에서 실행되므로 안전함
        String accessToken;
        try {
            accessToken = tokenClient.issueAccessToken(
                    appKey,
                    appSecret,
                    userApiKey.getServerType()).block(Duration.ofSeconds(30));

            if (accessToken == null) {
                throw new RuntimeException("토큰 발급 실패: userId=" + userId);
            }
        } catch (Exception e) {
            log.error("토큰 발급 중 오류 발생: userId={}", userId, e);
            throw new RuntimeException("토큰 발급 실패: userId=" + userId, e);
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
                    .serverType(serverType)
                    .accessTokenEncrypted(encryptedToken)
                    .expiresAt(expiresAt)
                    .issuedAt(LocalDateTime.now())
                    .build();
            tokenRepository.save(token);
        }

        // 최근 발급 이력 기록
        long now = System.currentTimeMillis();
        recentTokenIssuance.put(issuanceKey(userId, serverType), now);
        lastIssuanceTimeByUserId.put(userId, now);

        log.info("토큰 발급 및 저장 완료: userId={}, serverType={}", userId, serverType);
    }

    /**
     * 로그인 시 토큰이 DB에 없으면 1회 발급
     * 토큰이 이미 존재하면 발급하지 않습니다.
     * 
     * REQUIRES_NEW를 사용하여 별도 트랜잭션으로 실행되므로,
     * 토큰 발급 실패가 로그인 트랜잭션에 영향을 주지 않습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void issueTokenForUserIfNotExists(UserApiKey userApiKey) {
        String userId = userApiKey.getUserId();
        String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";

        // 기존 토큰 확인 (서버 타입별)
        boolean tokenExists = tokenRepository.findByUserIdAndServerType(userId, serverType).isPresent();

        if (tokenExists) {
            log.debug("토큰이 이미 존재함: userId={}, serverType={}", userId, serverType);
            return;
        }

        log.info("토큰이 없어서 발급 시작: userId={}, serverType={}", userId, serverType);
        issueTokenForUser(userApiKey);
    }

    /**
     * 사용자 ID로 토큰 조회 (복호화하여 반환).
     * 서버 타입 미지정 시 모의투자("1") 토큰 사용 (호환용).
     *
     * @deprecated 서버 타입별 조회 시 {@link #getAccessToken(String, String)} 사용
     */
    @Deprecated
    @Transactional
    public String getAccessToken(String userId) {
        return getAccessToken(userId, "1");
    }

    /**
     * 사용자 ID와 서버 타입으로 토큰 조회 (복호화하여 반환).
     * 토큰이 만료되었거나 복호화에 실패하면 자동으로 재발급합니다.
     *
     * @param userId     사용자 ID
     * @param serverType 서버 타입 ("1": 모의투자, "0": 실거래)
     * @return Access Token (평문)
     */
    @Transactional
    public String getAccessToken(String userId, String serverType) {
        String st = serverType != null ? serverType : "1";
        KoreaInvestmentToken token = tokenRepository.findByUserIdAndServerType(userId, st).orElse(null);

        // 토큰이 없거나 만료된 경우: 사용자 단위 락으로 직렬화 후 재발급
        if (token == null || !token.isValid()) {
            String key = issuanceKey(userId, st);
            Long recentIssuanceTime = recentTokenIssuance.get(key);
            if (recentIssuanceTime != null) {
                long timeSinceIssuance = System.currentTimeMillis() - recentIssuanceTime;
                if (timeSinceIssuance < RECENT_ISSUANCE_WINDOW_MS) {
                    log.debug("최근 토큰 발급 이력 확인됨, 재조회 시도: userId={}, serverType={}, timeSinceIssuance={}ms", userId, st, timeSinceIssuance);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    token = tokenRepository.findByUserIdAndServerType(userId, st).orElse(null);
                    if (token != null && token.isValid()) {
                        try {
                            return encryptionUtil.decrypt(token.getAccessTokenEncrypted());
                        } catch (RuntimeException e) {
                            log.warn("토큰 복호화 실패: userId={}, serverType={}, error={}", userId, st, e.getMessage());
                        }
                    }
                } else {
                    recentTokenIssuance.remove(key);
                }
            }

            if (token == null || !token.isValid()) {
                synchronized (lockForUser(userId)) {
                    token = tokenRepository.findByUserIdAndServerType(userId, st).orElse(null);
                    if (token != null && token.isValid()) {
                        try {
                            return encryptionUtil.decrypt(token.getAccessTokenEncrypted());
                        } catch (RuntimeException e) {
                            log.warn("토큰 복호화 실패: userId={}, serverType={}, error={}", userId, st, e.getMessage());
                            token = null;
                        }
                    }
                    if (token == null || !token.isValid()) {
                        Long lastByUser = lastIssuanceTimeByUserId.get(userId);
                        long now = System.currentTimeMillis();
                        if (lastByUser != null && (now - lastByUser) < TOKEN_ISSUANCE_COOLDOWN_MS) {
                            long waitSec = (TOKEN_ISSUANCE_COOLDOWN_MS - (now - lastByUser)) / 1000;
                            String msg = String.format(
                                    "접근토큰 발급은 1분당 1회만 가능합니다. 약 %d초 후 다시 시도해 주세요.",
                                    Math.max(1, waitSec));
                            log.warn("토큰 발급 제한: userId={}, serverType={}, lastIssuance={}ms ago", userId, st, now - lastByUser);
                            throw new RuntimeException(msg);
                        }

                        log.info("토큰이 없거나 만료됨, 재발급 시도: userId={}, serverType={}", userId, st);

                        UserApiKey userApiKey = userApiKeyRepository
                                .findByUserIdAndBrokerTypeAndServerType(userId, BrokerType.KOREA_INVESTMENT, st)
                                .orElseThrow(() -> new RuntimeException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId + ", serverType=" + st));

                        issueTokenForUser(userApiKey);

                        token = tokenRepository.findByUserIdAndServerType(userId, st)
                                .orElseThrow(() -> new RuntimeException("토큰 발급 후 조회 실패: userId=" + userId + ", serverType=" + st));
                    }
                }
            }
        }

        try {
            return encryptionUtil.decrypt(token.getAccessTokenEncrypted());
        } catch (RuntimeException e) {
            log.warn("토큰 복호화 실패, 토큰 삭제 후 재발급 시도: userId={}, serverType={}, error={}", userId, st, e.getMessage());

            tokenRepository.delete(token);

            UserApiKey userApiKey = userApiKeyRepository
                    .findByUserIdAndBrokerTypeAndServerType(userId, BrokerType.KOREA_INVESTMENT, st)
                    .orElseThrow(() -> new RuntimeException("한국투자증권 API 키를 찾을 수 없습니다: userId=" + userId));

            synchronized (lockForUser(userId)) {
                Long lastByUser = lastIssuanceTimeByUserId.get(userId);
                long now = System.currentTimeMillis();
                if (lastByUser != null && (now - lastByUser) < TOKEN_ISSUANCE_COOLDOWN_MS) {
                    long waitSec = (TOKEN_ISSUANCE_COOLDOWN_MS - (now - lastByUser)) / 1000;
                    throw new RuntimeException(String.format(
                            "접근토큰 발급은 1분당 1회만 가능합니다. 약 %d초 후 다시 시도해 주세요.", Math.max(1, waitSec)));
                }
                issueTokenForUser(userApiKey);
            }

            token = tokenRepository.findByUserIdAndServerType(userId, st)
                    .orElseThrow(() -> new RuntimeException("토큰 발급 후 조회 실패: userId=" + userId));

            return encryptionUtil.decrypt(token.getAccessTokenEncrypted());
        }
    }
}
