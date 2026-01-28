package com.investment.auth.service;

import com.investment.auth.dto.*;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.JwtTokenProvider;
import com.investment.common.security.LogMaskingUtil;
import com.investment.common.security.SecurityAuditService;
import com.investment.common.util.AccountNumberUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.User;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserAccountRepository;
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
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EncryptionUtil encryptionUtil;
    private final KoreaInvestmentTokenService tokenService;
    private final SecurityAuditService securityAuditService;
    private final AccountLockService accountLockService;

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
        // fromCode()가 OTHER를 반환하고 원래 코드가 "OTHER"가 아닌 경우 → 지원하지 않는 증권사
        if (brokerType == BrokerType.OTHER && !"OTHER".equals(request.getBrokerType())) {
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
                LogMaskingUtil.maskUserId(user.getId()),
                LogMaskingUtil.maskUsername(user.getUsername()),
                brokerType);

        // 한국투자증권인 경우 토큰 자동 발급 및 계좌번호 저장
        // 별도 트랜잭션으로 처리하여 토큰 발급 실패 시에도 회원가입은 성공하도록 함
        if (brokerType == BrokerType.KOREA_INVESTMENT) {
            try {
                // 계좌번호 필수 검증 및 형식 검증
                String accountNo = request.getAccountNo();
                if (accountNo == null || accountNo.trim().isEmpty()) {
                    throw new DomainException(ErrorCode.INVALID_INPUT, "계좌번호는 필수입니다");
                }

                accountNo = accountNo.trim();

                // 계좌번호 형식 검증
                if (!AccountNumberUtil.validateAccountNumberFormat(accountNo)) {
                    throw new DomainException(ErrorCode.INVALID_INPUT,
                            "계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12)");
                }

                // 계좌번호 암호화
                String accountNoEncrypted = encryptionUtil.encrypt(accountNo);

                // 계좌 별칭 생성 (증권사명 + 계좌번호 일부)
                String accountName = brokerType.getName() + " " + maskAccountNo(accountNo);

                // 메인 계좌 여부 확인 (첫 계좌는 자동으로 메인 계좌)
                boolean isMainAccount = !userAccountRepository.existsByUserIdAndIsDefaultTrue(user.getId());

                // UserAccount 생성 및 저장
                UserAccount userAccount = UserAccount.builder()
                        .userId(user.getId())
                        .userApiKeyId(userApiKey.getId())
                        .accountNoEncrypted(accountNoEncrypted)
                        .brokerType(brokerType)
                        .accountName(accountName)
                        .isDefault(isMainAccount)
                        .isActive(true)
                        .build();
                userAccountRepository.save(userAccount);

                log.info("계좌번호 저장 완료: userId={}, accountNo={}, isMainAccount={}",
                        LogMaskingUtil.maskUserId(user.getId()),
                        maskAccountNo(accountNo),
                        isMainAccount);

                // 토큰 발급
                tokenService.issueTokenForUserInNewTransaction(userApiKey);
                log.info("한국투자증권 토큰 발급 완료: userId={}", LogMaskingUtil.maskUserId(user.getId()));
            } catch (DomainException e) {
                // DomainException은 그대로 전파
                throw e;
            } catch (Exception e) {
                log.error("한국투자증권 토큰 발급 실패: userId={}, error={}",
                        LogMaskingUtil.maskUserId(user.getId()), e.getMessage(), e);
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
    public AuthResponseDto login(LoginRequestDto request, String ipAddress) {
        // 계정 잠금 확인
        accountLockService.throwIfLocked(request.getUsername());

        // 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    // 인증 실패 기록 및 잠금 확인
                    boolean locked = accountLockService.recordFailureAndCheckLock(request.getUsername());

                    // 인증 실패 로깅
                    securityAuditService.logAuthenticationFailure(
                            request.getUsername(),
                            "USER_NOT_FOUND",
                            ipAddress != null ? ipAddress : "unknown");

                    String message = locked ? "계정이 잠금되었습니다. 잠시 후 다시 시도해주세요." : "사용자 ID 또는 비밀번호가 올바르지 않습니다";

                    return new DomainException(ErrorCode.USER_NOT_FOUND, message);
                });

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 인증 실패 기록 및 잠금 확인
            boolean locked = accountLockService.recordFailureAndCheckLock(request.getUsername());

            // 인증 실패 로깅
            securityAuditService.logAuthenticationFailure(
                    request.getUsername(),
                    "INVALID_PASSWORD",
                    ipAddress != null ? ipAddress : "unknown");

            String message = locked ? "계정이 잠금되었습니다. 잠시 후 다시 시도해주세요." : "사용자 ID 또는 비밀번호가 올바르지 않습니다";

            throw new DomainException(ErrorCode.INVALID_PASSWORD, message);
        }

        // 인증 성공 시 계정 잠금 해제
        accountLockService.unlockAccount(request.getUsername());

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);

        // 인증 성공 로깅
        securityAuditService.logAuthenticationSuccess(
                user.getId(),
                user.getUsername(),
                ipAddress != null ? ipAddress : "unknown");

        log.info("로그인 성공: userId={}, username={}",
                LogMaskingUtil.maskUserId(user.getId()),
                LogMaskingUtil.maskUsername(user.getUsername()));

        // 한국투자증권 사용자인 경우 토큰 체크 및 발급
        // 별도 트랜잭션으로 실행되므로, 토큰 발급 실패가 로그인 트랜잭션에 영향을 주지 않음
        List<UserApiKey> userApiKeys = userApiKeyRepository.findByUserId(user.getId());
        for (UserApiKey userApiKey : userApiKeys) {
            if (userApiKey.getBrokerType() == BrokerType.KOREA_INVESTMENT) {
                try {
                    // 토큰이 DB에 없으면 1회 발급 (별도 트랜잭션으로 실행)
                    tokenService.issueTokenForUserIfNotExists(userApiKey);
                } catch (Exception e) {
                    log.error("한국투자증권 토큰 발급 실패: userId={}, error={}",
                            LogMaskingUtil.maskUserId(user.getId()), e.getMessage());
                    // 토큰 발급 실패해도 로그인은 성공으로 처리
                    // 사용자는 마이페이지에서 API 키를 다시 입력하여 문제를 해결할 수 있습니다
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

        // 민감한 작업(비밀번호 변경, API 키 변경) 시 재인증 확인
        boolean requiresReauthentication = (request.getPassword() != null && !request.getPassword().isEmpty()) ||
                (request.getAppKey() != null && !request.getAppKey().isEmpty()) ||
                (request.getAppSecret() != null && !request.getAppSecret().isEmpty());

        if (requiresReauthentication) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new DomainException(ErrorCode.INVALID_PASSWORD, "비밀번호 변경 또는 API 키 변경 시 현재 비밀번호가 필요합니다");
            }

            // 현재 비밀번호 확인
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                securityAuditService.logAuthenticationFailure(
                        user.getUsername(),
                        "REAUTHENTICATION_FAILED",
                        "unknown");
                throw new DomainException(ErrorCode.INVALID_PASSWORD, "현재 비밀번호가 올바르지 않습니다");
            }
        }

        // 비밀번호 변경
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.updatePassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            log.info("비밀번호 변경: userId={}", LogMaskingUtil.maskUserId(userId));
            securityAuditService.logPasswordChanged(userId, user.getUsername());
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
            // fromCode()가 OTHER를 반환하고 원래 코드가 "OTHER"가 아닌 경우 → 지원하지 않는 증권사
            if (newBrokerType == BrokerType.OTHER && !"OTHER".equals(request.getBrokerType())) {
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
            log.info("API 키 정보 변경: userId={}", LogMaskingUtil.maskUserId(userId));
            securityAuditService.logApiKeyChanged(userId, user.getUsername());
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

    /**
     * 계좌번호 마스킹 (뒤 4자리만 표시)
     */
    private String maskAccountNo(String accountNo) {
        if (accountNo == null || accountNo.length() <= 4) {
            return "****";
        }
        int length = accountNo.length();
        return "*".repeat(Math.max(0, length - 4)) + accountNo.substring(length - 4);
    }
}
