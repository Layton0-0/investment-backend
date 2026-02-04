package com.investment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 리포지토리가 Spring Boot 기본 EntityManagerFactory를 사용하도록 명시.
 * Spring Batch 사용 시 Batch가 등록하는 jpaSharedEM_entityManagerFactory 대신
 * Boot가 등록하는 entityManagerFactory를 사용해 빈 참조 오류를 방지한다.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.investment.domain.repository", entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
public class JpaRepositoriesConfig {
}
