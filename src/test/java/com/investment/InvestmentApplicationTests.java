package com.investment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 전체 애플리케이션 컨텍스트 로드 테스트.
 * 외부 서비스(PostgreSQL, Redis)가 필요하므로 로컬/통합 테스트 환경에서만 실행.
 * CI 환경에서는 docker-compose로 외부 서비스 구동 후 실행하거나,
 * Testcontainers 사용을 고려.
 */
@SpringBootTest
@Disabled("외부 서비스(DB, Redis) 필요 - 로컬 통합 테스트 환경에서 실행")
class InvestmentApplicationTests {

    @Test
    void contextLoads() {
    }
}
