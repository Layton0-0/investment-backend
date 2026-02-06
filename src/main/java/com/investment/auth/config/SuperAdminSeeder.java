package com.investment.auth.config;

import com.investment.common.security.LogMaskingUtil;
import com.investment.domain.entity.User;
import com.investment.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 슈퍼관리자(yoon) 비밀번호 동기화.
 * investment.security.super-admin.password(또는 env SUPER_ADMIN_PASSWORD)가 설정된 경우에만
 * 해당 계정의 비밀번호를 설정한 값으로 한 번 갱신한다. 역할(Admin)은 V26 마이그레이션에서 설정됨.
 */
@Slf4j
@Component
@Order(100)
public class SuperAdminSeeder implements ApplicationRunner {

    private static final String SUPER_ADMIN_USERNAME = "yoon";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String superAdminPassword;

    public SuperAdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${investment.security.super-admin.password:}") String superAdminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.superAdminPassword = superAdminPassword != null ? superAdminPassword : "";
    }

    @Override
    public void run(ApplicationArguments args) {
        if (superAdminPassword.isBlank()) {
            return;
        }
        userRepository.findByUsername(SUPER_ADMIN_USERNAME)
                .ifPresent(user -> {
                    user.updatePassword(passwordEncoder.encode(superAdminPassword));
                    userRepository.save(user);
                    log.info("슈퍼관리자 비밀번호 동기화 완료: username={}", LogMaskingUtil.maskUsername(SUPER_ADMIN_USERNAME));
                });
    }
}
