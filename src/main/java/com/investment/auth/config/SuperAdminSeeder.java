package com.investment.auth.config;

import com.investment.common.security.EncryptionUtil;
import com.investment.common.security.LogMaskingUtil;
import com.investment.common.util.AccountNumberUtil;
import com.investment.domain.entity.BrokerType;
import com.investment.domain.entity.User;
import com.investment.domain.entity.UserAccount;
import com.investment.domain.entity.UserApiKey;
import com.investment.domain.repository.UserAccountRepository;
import com.investment.domain.repository.UserApiKeyRepository;
import com.investment.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 슈퍼관리자 계정·비밀번호 및 모의/실계좌 정보 동기화.
 * username·password가 설정된 경우: DB에 해당 계정이 없으면 생성(role=Admin), 있으면 비밀번호만 갱신.
 * virtual/real의 app-key·app-secret이 설정된 경우 한국투자증권 API 키·계좌(account-no 선택)를 기동 시 한 번 갱신한다.
 * 회원가입 API 없이 env만으로 슈퍼관리자를 보장한다.
 */
@Slf4j
@Component
@Order(100)
public class SuperAdminSeeder implements ApplicationRunner {

    private static final BrokerType BROKER = BrokerType.KOREA_INVESTMENT;
    private static final String SERVER_TYPE_VIRTUAL = "1";
    private static final String SERVER_TYPE_REAL = "0";
    private static final String SUPER_ADMIN_ROLE = "Admin";

    private final UserRepository userRepository;
    private final UserApiKeyRepository userApiKeyRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final String superAdminUsername;
    private final String superAdminPassword;
    private final String virtualAppKey;
    private final String virtualAppSecret;
    private final String virtualAccountNo;
    private final String realAppKey;
    private final String realAppSecret;
    private final String realAccountNo;

    public SuperAdminSeeder(
            UserRepository userRepository,
            UserApiKeyRepository userApiKeyRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            EncryptionUtil encryptionUtil,
            @Value("${investment.security.super-admin.username:}") String superAdminUsername,
            @Value("${investment.security.super-admin.password:}") String superAdminPassword,
            @Value("${investment.security.super-admin.virtual.app-key:}") String virtualAppKey,
            @Value("${investment.security.super-admin.virtual.app-secret:}") String virtualAppSecret,
            @Value("${investment.security.super-admin.virtual.account-no:}") String virtualAccountNo,
            @Value("${investment.security.super-admin.real.app-key:}") String realAppKey,
            @Value("${investment.security.super-admin.real.app-secret:}") String realAppSecret,
            @Value("${investment.security.super-admin.real.account-no:}") String realAccountNo) {
        this.userRepository = userRepository;
        this.userApiKeyRepository = userApiKeyRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.encryptionUtil = encryptionUtil;
        this.superAdminUsername = superAdminUsername != null ? superAdminUsername.trim() : "";
        this.superAdminPassword = superAdminPassword != null ? superAdminPassword : "";
        this.virtualAppKey = blankToEmpty(virtualAppKey);
        this.virtualAppSecret = blankToEmpty(virtualAppSecret);
        this.virtualAccountNo = blankToEmpty(virtualAccountNo);
        this.realAppKey = blankToEmpty(realAppKey);
        this.realAppSecret = blankToEmpty(realAppSecret);
        this.realAccountNo = blankToEmpty(realAccountNo);
    }

    private static String blankToEmpty(String s) {
        return s != null ? s.trim() : "";
    }

    @Override
    public void run(ApplicationArguments args) {
        if (superAdminUsername.isBlank() || superAdminPassword.isBlank()) {
            return;
        }
        User user;
        Optional<User> existing = userRepository.findByUsername(superAdminUsername);
        if (existing.isPresent()) {
            user = existing.get();
            user.updatePassword(passwordEncoder.encode(superAdminPassword));
            user.updateRole(SUPER_ADMIN_ROLE);
            userRepository.save(user);
            log.info("슈퍼관리자 비밀번호·역할 동기화 완료: username={}", LogMaskingUtil.maskUsername(superAdminUsername));
        } else {
            user = createSuperAdmin();
        }
        syncAccount(user.getId(), SERVER_TYPE_VIRTUAL, virtualAppKey, virtualAppSecret, virtualAccountNo);
        syncAccount(user.getId(), SERVER_TYPE_REAL, realAppKey, realAppSecret, realAccountNo);
    }

    private User createSuperAdmin() {
        User newUser = User.builder()
                .username(superAdminUsername)
                .passwordHash(passwordEncoder.encode(superAdminPassword))
                .role(SUPER_ADMIN_ROLE)
                .build();
        User saved = userRepository.save(newUser);
        log.info("슈퍼관리자 계정 자동 생성: username={}", LogMaskingUtil.maskUsername(superAdminUsername));
        return saved;
    }

    private void syncAccount(String userId, String serverType, String appKey, String appSecret, String accountNo) {
        if (appKey.isBlank() || appSecret.isBlank()) {
            return;
        }
        String appKeyEncrypted = encryptionUtil.encrypt(appKey);
        String appSecretEncrypted = encryptionUtil.encrypt(appSecret);

        Optional<UserApiKey> existingKey = userApiKeyRepository.findByUserIdAndBrokerTypeAndServerType(userId, BROKER, serverType);
        UserApiKey apiKey;
        if (existingKey.isPresent()) {
            apiKey = existingKey.get();
            apiKey.updateApiKeys(appKeyEncrypted, appSecretEncrypted, serverType);
            userApiKeyRepository.save(apiKey);
        } else {
            apiKey = UserApiKey.builder()
                    .userId(userId)
                    .brokerType(BROKER)
                    .appKeyEncrypted(appKeyEncrypted)
                    .appSecretEncrypted(appSecretEncrypted)
                    .serverType(serverType)
                    .build();
            apiKey = userApiKeyRepository.save(apiKey);
        }

        if (!accountNo.isBlank()) {
            if (!AccountNumberUtil.validateAccountNumberFormat(accountNo)) {
                log.warn("슈퍼관리자 계좌번호 형식 무시: serverType={}, accountNo={}", serverType, LogMaskingUtil.maskAccountNo(accountNo));
                return;
            }
            String accountNoEncrypted = encryptionUtil.encrypt(accountNo.trim());
            String accountName = BROKER.getName() + " " + LogMaskingUtil.maskAccountNo(accountNo);

            String apiKeyId = apiKey.getId();
            List<UserAccount> accounts = userAccountRepository.findByUserIdAndBrokerTypeAndServerType(userId, BROKER, serverType);
            Optional<UserAccount> sameApiKeyAccount = accounts.stream()
                    .filter(a -> apiKeyId.equals(a.getUserApiKeyId()))
                    .findFirst();
            if (sameApiKeyAccount.isPresent()) {
                UserAccount acc = sameApiKeyAccount.get();
                acc.updateAccountNo(accountNoEncrypted);
                acc.updateAccountName(accountName);
                userAccountRepository.save(acc);
            } else {
                boolean isMain = !userAccountRepository.existsByUserIdAndServerTypeAndIsDefaultTrue(userId, serverType);
                UserAccount newAccount = UserAccount.builder()
                        .userId(userId)
                        .userApiKeyId(apiKey.getId())
                        .accountNoEncrypted(accountNoEncrypted)
                        .brokerType(BROKER)
                        .serverType(serverType)
                        .accountName(accountName)
                        .isDefault(isMain)
                        .isActive(true)
                        .build();
                userAccountRepository.save(newAccount);
            }
            log.info("슈퍼관리자 계좌 동기화 완료: serverType={}, accountNo={}", serverType, LogMaskingUtil.maskAccountNo(accountNo));
        }
    }
}
