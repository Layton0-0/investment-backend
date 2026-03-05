package com.investment.config;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 직렬화 공통 설정.
 * Java 8 date/time 타입(Instant, LocalDateTime 등) 지원을 위해 JavaTimeModule 등록.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeModuleCustomizer() {
        return builder -> builder.modulesToInstall(new JavaTimeModule());
    }
}
