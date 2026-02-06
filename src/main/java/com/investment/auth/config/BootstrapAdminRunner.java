package com.investment.auth.config;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.User;
import com.investment.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 최초 관리자 부트스트랩.
 * ADMIN 계정이 0건일 때만 1회, 설정된 username/password로 User(role=Admin)를 생성한다.
 * investment.security.bootstrap-admin.enabled=true 및 username/password 설정 시에만 동작.
 */
@Slf4j
@Component
@Order(90)
@ConditionalOnProperty(name = "investment.security.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final String ADMIN_ROLE = "Admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapUsername;
    private final String bootstrapPassword;

    public BootstrapAdminRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${investment.security.bootstrap-admin.username:}") String bootstrapUsername,
            @Value("${investment.security.bootstrap-admin.password:}") String bootstrapPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapUsername = bootstrapUsername != null ? bootstrapUsername.trim() : "";
        this.bootstrapPassword = bootstrapPassword != null ? bootstrapPassword : "";
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() || bootstrapPassword.isBlank()) {
            log.debug("부트스트랩 관리자: username 또는 password 미설정으로 스킵");
            return;
        }
        if (userRepository.countByRole(ADMIN_ROLE) > 0) {
            log.debug("부트스트랩 관리자: 이미 ADMIN 계정이 존재하여 스킵");
            return;
        }
        if (userRepository.existsByUsername(bootstrapUsername)) {
            log.warn("부트스트랩 관리자: username={} 이미 존재하여 ADMIN 부트스트랩 스킵",
                    LogMaskingUtil.maskUsername(bootstrapUsername));
            return;
        }
        User admin = User.builder()
                .username(bootstrapUsername)
                .passwordHash(passwordEncoder.encode(bootstrapPassword))
                .role(ADMIN_ROLE)
                .build();
        userRepository.save(admin);
        log.info("부트스트랩 관리자 생성 완료: username={}", LogMaskingUtil.maskUsername(bootstrapUsername));
    }
}
