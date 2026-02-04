package com.investment.config;

import org.flywaydb.core.api.exception.FlywayValidateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flyway 검증 실패 시 repair 후 migrate 재시도 (로컬/개발 전용).
 * "Detected failed migration to version N" 발생 시 schema history를 repair로 정리한 뒤
 * 재기동할 수 있게 한다.
 * 프로덕션에서는 사용하지 않도록 repair-on-validate-failure는 local 프로필에서만 true로 설정한다.
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.repair-on-validate-failure", havingValue = "true")
public class FlywayRepairOnValidateFailureConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairOnValidateFailureConfig.class);

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            try {
                flyway.migrate();
            } catch (FlywayValidateException e) {
                log.warn(
                        "Flyway validate failed (e.g. failed migration in history). Running repair then migrate. Cause: {}",
                        e.getMessage());
                flyway.repair();
                flyway.migrate();
            }
        };
    }
}
