package com.investment.auth.service;

import com.investment.auth.dto.*;
import com.investment.setting.dto.*;
import com.investment.common.exception.DomainException;
import com.investment.common.exception.ErrorCode;
import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.JwtTokenProvider;
import com.investment.common.security.LogMaskingUtil;
import com.investment.common.security.SecurityAuditService;
import com.investment.common.util.AccountNumberUtil;
import com.investment.common.validation.PasswordValidator;
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
        if (log.isDebugEnabled()) {
            log.debug("  [DEBUG] userId(actual)={}, username(actual)={}", user.getId(), user.getUsername());
        }

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
                String accountName = brokerType.getName() + " " + LogMaskingUtil.maskAccountNo(accountNo);

                // 서버 타입 (모의/실거래) - API 키와 동일하게 설정
                String serverType = userApiKey.getServerType() != null ? userApiKey.getServerType() : "1";
                // 메인 계좌 여부 확인 (같은 서버 타입 내 첫 계좌는 자동으로 메인 계좌)
                boolean isMainAccount = !userAccountRepository.existsByUserIdAndServerTypeAndIsDefaultTrue(user.getId(),
                        serverType);

                // UserAccount 생성 및 저장
                UserAccount userAccount = UserAccount.builder()
                        .userId(user.getId())
                        .userApiKeyId(userApiKey.getId())
                        .accountNoEncrypted(accountNoEncrypted)
                        .brokerType(brokerType)
                        .serverType(serverType)
                        .accountName(accountName)
                        .isDefault(isMainAccount)
                        .isActive(true)
                        .build();
                userAccountRepository.save(userAccount);

                log.info("계좌번호 저장 완료: userId={}, accountNo={}, isMainAccount={}",
                        LogMaskingUtil.maskUserId(user.getId()),
                        LogMaskingUtil.maskAccountNo(accountNo),
                        isMainAccount);
                if (log.isDebugEnabled()) {
                    log.debug("  [DEBUG] userId(actual)={}, accountNo(actual)={}", user.getId(), accountNo);
                }

                // 토큰: 계좌인증 시 미리 발급받은 토큰이 있으면 저장만, 없으면 발급
                String preIssued = request.getPreIssuedAccessToken();
                if (preIssued != null && !preIssued.isBlank()) {
                    tokenService.savePreIssuedTokenForUser(user.getId(), serverType, preIssued.trim());
                    log.info("한국투자증권 선발급 토큰 저장 완료: userId={}", LogMaskingUtil.maskUserId(user.getId()));
                    if (log.isDebugEnabled()) {
                        log.debug("  [DEBUG] userId(actual)={}", user.getId());
                    }
                } else {
                    tokenService.issueTokenForUserInNewTransaction(userApiKey);
                    log.info("한국투자증권 토큰 발급 완료: userId={}", LogMaskingUtil.maskUserId(user.getId()));
                    if (log.isDebugEnabled()) {
                        log.debug("  [DEBUG] userId(actual)={}", user.getId());
                    }
                }
            } catch (DomainException e) {
                // DomainException은 그대로 전파
                throw e;
            } catch (Exception e) {
                log.error("한국투자증권 토큰 발급 실패: userId={}, error={}",
                        LogMaskingUtil.maskUserId(user.getId()), e.getMessage(), e);
                if (log.isDebugEnabled()) {
                    log.debug("  [DEBUG] userId(actual)={}", user.getId());
                }
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
        if (log.isDebugEnabled()) {
            log.debug("  [DEBUG] userId(actual)={}, username(actual)={}", user.getId(), user.getUsername());
        }

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
                    if (log.isDebugEnabled()) {
                        log.debug("  [DEBUG] userId(actual)={}", user.getId());
                    }
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

        // 메인 계좌 조회 (계좌번호 마스킹)
        String accountNoMasked = null;
        UserAccount mainAccount = userAccountRepository.findByUserIdAndIsDefaultTrue(userId).orElse(null);
        if (mainAccount != null) {
            String accountNo = encryptionUtil.decrypt(mainAccount.getAccountNoEncrypted());
            accountNoMasked = LogMaskingUtil.maskAccountNo(accountNo);
        }

        return MyPageResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .brokerType(userApiKey.getBrokerType().getCode())
                .brokerTypeName(userApiKey.getBrokerType().getName())
                .appKeyMasked(LogMaskingUtil.maskApiKey(appKey))
                .appSecretMasked(LogMaskingUtil.maskSecret(appSecret))
                .serverType(userApiKey.getServerType())
                .serverTypeName("1".equals(userApiKey.getServerType()) ? "모의투자" : "실거래")
                .accountNoMasked(accountNoMasked)
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

        // 민감한 작업(비밀번호 변경, API 키 변경) 시 재인증 확인 (계좌번호 변경은 제외)
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
            // 새 비밀번호 검증
            PasswordValidator.ValidationResult passwordValidation = PasswordValidator
                    .validatePassword(request.getPassword());
            if (!passwordValidation.isValid()) {
                throw new DomainException(ErrorCode.INVALID_PASSWORD, passwordValidation.getMessage());
            }

            user.updatePassword(passwordEncoder.encode(request.getPassword()));
            userRepository.save(user);
            log.info("비밀번호 변경: userId={}", LogMaskingUtil.maskUserId(userId));
            if (log.isDebugEnabled()) {
                log.debug("  [DEBUG] userId(actual)={}", userId);
            }
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
            if (log.isDebugEnabled()) {
                log.debug("  [DEBUG] userId(actual)={}", userId);
            }
            securityAuditService.logApiKeyChanged(userId, user.getUsername());
        }

        // 계좌번호 변경 처리
        String accountNoMasked = null;
        if (request.getAccountNo() != null && !request.getAccountNo().trim().isEmpty()) {
            String accountNo = request.getAccountNo().trim();

            // 계좌번호 형식 검증
            if (!AccountNumberUtil.validateAccountNumberFormat(accountNo)) {
                throw new DomainException(ErrorCode.INVALID_INPUT,
                        "계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12)");
            }

            // 계좌번호 암호화
            String accountNoEncrypted = encryptionUtil.encrypt(accountNo);

            // 같은 서버 타입 내 메인 계좌 조회 또는 생성 (모의/실거래 계좌 구분)
            UserAccount mainAccount = userAccountRepository
                    .findByUserIdAndServerTypeAndIsDefaultTrue(userId, serverType).orElse(null);
            if (mainAccount != null) {
                // 기존 메인 계좌 업데이트
                mainAccount.updateAccountNo(accountNoEncrypted);
                // 계좌 별칭도 업데이트
                String accountName = brokerType.getName() + " " + LogMaskingUtil.maskAccountNo(accountNo);
                mainAccount.updateAccountName(accountName);
                userAccountRepository.save(mainAccount);
                log.info("계좌번호 변경: userId={}, accountNo={}, serverType={}",
                        LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), serverType);
                if (log.isDebugEnabled()) {
                    log.debug("  [DEBUG] userId(actual)={}, accountNo(actual)={}", userId, accountNo);
                }
            } else {
                // 메인 계좌가 없으면 새로 생성
                String accountName = brokerType.getName() + " " + LogMaskingUtil.maskAccountNo(accountNo);
                mainAccount = UserAccount.builder()
                        .userId(userId)
                        .userApiKeyId(userApiKey.getId())
                        .brokerType(brokerType)
                        .serverType(serverType)
                        .accountNoEncrypted(accountNoEncrypted)
                        .accountName(accountName)
                        .isDefault(true)
                        .isActive(true)
                        .build();
                userAccountRepository.save(mainAccount);
                log.info("계좌번호 신규 등록: userId={}, accountNo={}, serverType={}",
                        LogMaskingUtil.maskUserId(userId), LogMaskingUtil.maskAccountNo(accountNo), serverType);
                if (log.isDebugEnabled()) {
                    log.debug("  [DEBUG] userId(actual)={}, accountNo(actual)={}", userId, accountNo);
                }
            }

            accountNoMasked = LogMaskingUtil.maskAccountNo(accountNo);
        } else {
            // 계좌번호 변경이 없으면 기존 계좌번호 마스킹 조회 (같은 서버 타입 메인 계좌)
            UserAccount mainAccount = userAccountRepository
                    .findByUserIdAndServerTypeAndIsDefaultTrue(userId, serverType).orElse(null);
            if (mainAccount != null) {
                String accountNo = encryptionUtil.decrypt(mainAccount.getAccountNoEncrypted());
                accountNoMasked = LogMaskingUtil.maskAccountNo(accountNo);
            }
        }

        // 응답 생성
        String appKey = encryptionUtil.decrypt(appKeyEncrypted);
        String appSecret = encryptionUtil.decrypt(appSecretEncrypted);

        return MyPageResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .brokerType(brokerType.getCode())
                .brokerTypeName(brokerType.getName())
                .appKeyMasked(LogMaskingUtil.maskApiKey(appKey))
                .appSecretMasked(LogMaskingUtil.maskSecret(appSecret))
                .serverType(serverType)
                .serverTypeName("1".equals(serverType) ? "모의투자" : "실거래")
                .accountNoMasked(accountNoMasked)
                .build();
    }

    /**
     * 설정 화면용 계좌 정보 한번에 조회 (모의·실 두 블록)
     */
    @Transactional(readOnly = true)
    public SettingsAccountsResponseDto getSettingsAccounts(String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

        SettingsAccountBlockDto virtual = buildSettingsBlock(userId, "1");
        SettingsAccountBlockDto real = buildSettingsBlock(userId, "0");

        return SettingsAccountsResponseDto.builder()
                .virtual(virtual)
                .real(real)
                .build();
    }

    private SettingsAccountBlockDto buildSettingsBlock(String userId, String serverType) {
        var apiKeyOpt = userApiKeyRepository.findByUserIdAndBrokerTypeAndServerType(
                userId, BrokerType.KOREA_INVESTMENT, serverType);
        if (apiKeyOpt.isEmpty()) {
            return SettingsAccountBlockDto.builder()
                    .appKeyMasked(null)
                    .appSecretMasked(null)
                    .accountNoMasked(null)
                    .hasApiKey(false)
                    .build();
        }
        UserApiKey key = apiKeyOpt.get();
        String appKey = encryptionUtil.decrypt(key.getAppKeyEncrypted());
        String appSecret = encryptionUtil.decrypt(key.getAppSecretEncrypted());
        String accountNoMasked = null;
        var accountOpt = userAccountRepository.findByUserIdAndServerTypeAndIsDefaultTrue(userId, serverType);
        if (accountOpt.isPresent()) {
            String accountNo = encryptionUtil.decrypt(accountOpt.get().getAccountNoEncrypted());
            accountNoMasked = LogMaskingUtil.maskAccountNo(accountNo);
        }
        return SettingsAccountBlockDto.builder()
                .appKeyMasked(LogMaskingUtil.maskApiKey(appKey))
                .appSecretMasked(LogMaskingUtil.maskSecret(appSecret))
                .accountNoMasked(accountNoMasked)
                .hasApiKey(true)
                .build();
    }

    /**
     * 설정 화면용 계좌 정보 한번에 수정 (모의·실 두 블록)
     */
    @Transactional
    public SettingsAccountsResponseDto updateSettingsAccounts(String userId, SettingsAccountsUpdateRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

        boolean anyApiKeyChange = hasApiKeyChange(request.getVirtual()) || hasApiKeyChange(request.getReal());
        if (anyApiKeyChange) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new DomainException(ErrorCode.INVALID_PASSWORD, "API 키 변경 시 현재 비밀번호가 필요합니다");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                securityAuditService.logAuthenticationFailure(user.getUsername(), "REAUTHENTICATION_FAILED", "unknown");
                throw new DomainException(ErrorCode.INVALID_PASSWORD, "현재 비밀번호가 올바르지 않습니다");
            }
        }

        if (request.getVirtual() != null) {
            applySettingsBlock(userId, "1", request.getVirtual());
        }
        if (request.getReal() != null) {
            applySettingsBlock(userId, "0", request.getReal());
        }

        return getSettingsAccounts(userId);
    }

    private boolean hasApiKeyChange(SettingsAccountBlockRequestDto block) {
        return block != null && ((block.getAppKey() != null && !block.getAppKey().isEmpty())
                || (block.getAppSecret() != null && !block.getAppSecret().isEmpty()));
    }

    private void applySettingsBlock(String userId, String serverType, SettingsAccountBlockRequestDto block) {
        boolean hasKey = (block.getAppKey() != null && !block.getAppKey().trim().isEmpty())
                || (block.getAppSecret() != null && !block.getAppSecret().trim().isEmpty());
        boolean hasAccountNo = block.getAccountNo() != null && !block.getAccountNo().trim().isEmpty();

        var apiKeyOpt = userApiKeyRepository.findByUserIdAndBrokerTypeAndServerType(
                userId, BrokerType.KOREA_INVESTMENT, serverType);

        UserApiKey userApiKey;
        if (apiKeyOpt.isPresent()) {
            userApiKey = apiKeyOpt.get();
            if (block.getAppKey() != null && !block.getAppKey().trim().isEmpty()) {
                userApiKey.updateApiKeys(
                        encryptionUtil.encrypt(block.getAppKey().trim()),
                        userApiKey.getAppSecretEncrypted(),
                        serverType);
            }
            if (block.getAppSecret() != null && !block.getAppSecret().trim().isEmpty()) {
                userApiKey.updateApiKeys(
                        userApiKey.getAppKeyEncrypted(),
                        encryptionUtil.encrypt(block.getAppSecret().trim()),
                        serverType);
            }
            userApiKeyRepository.save(userApiKey);
        } else if (hasKey) {
            String appKeyEnc = block.getAppKey() != null && !block.getAppKey().trim().isEmpty()
                    ? encryptionUtil.encrypt(block.getAppKey().trim()) : null;
            String appSecretEnc = block.getAppSecret() != null && !block.getAppSecret().trim().isEmpty()
                    ? encryptionUtil.encrypt(block.getAppSecret().trim()) : null;
            if (appKeyEnc == null || appSecretEnc == null) {
                throw new DomainException(ErrorCode.INVALID_INPUT, "모의/실 계좌 신규 등록 시 API Key와 Secret을 모두 입력해야 합니다");
            }
            userApiKey = UserApiKey.builder()
                    .userId(userId)
                    .brokerType(BrokerType.KOREA_INVESTMENT)
                    .appKeyEncrypted(appKeyEnc)
                    .appSecretEncrypted(appSecretEnc)
                    .serverType(serverType)
                    .build();
            userApiKeyRepository.save(userApiKey);
        } else {
            if (hasAccountNo) {
                throw new DomainException(ErrorCode.INVALID_INPUT, "해당 서버타입에 API 키를 먼저 등록한 뒤 계좌번호를 입력하세요");
            }
            return;
        }

        if (hasAccountNo) {
            String accountNo = block.getAccountNo().trim();
            if (!AccountNumberUtil.validateAccountNumberFormat(accountNo)) {
                throw new DomainException(ErrorCode.INVALID_INPUT,
                        "계좌번호 형식이 올바르지 않습니다. 형식: 숫자8자리-숫자2자리 (예: 12345678-12)");
            }
            String accountNoEncrypted = encryptionUtil.encrypt(accountNo);
            var mainOpt = userAccountRepository.findByUserIdAndServerTypeAndIsDefaultTrue(userId, serverType);
            String accountName = BrokerType.KOREA_INVESTMENT.getName() + " " + LogMaskingUtil.maskAccountNo(accountNo);
            if (mainOpt.isPresent()) {
                UserAccount acc = mainOpt.get();
                acc.updateAccountNo(accountNoEncrypted);
                acc.updateAccountName(accountName);
                userAccountRepository.save(acc);
            } else {
                UserAccount newAccount = UserAccount.builder()
                        .userId(userId)
                        .userApiKeyId(userApiKey.getId())
                        .accountNoEncrypted(accountNoEncrypted)
                        .brokerType(BrokerType.KOREA_INVESTMENT)
                        .serverType(serverType)
                        .accountName(accountName)
                        .isDefault(true)
                        .isActive(true)
                        .build();
                userAccountRepository.save(newAccount);
            }
            log.info("계좌번호 저장: userId={}, serverType={}, accountNo={}",
                    LogMaskingUtil.maskUserId(userId), serverType, LogMaskingUtil.maskAccountNo(accountNo));
        }
    }

}
