package com.investment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger 설정
 */
@Configuration
public class OpenApiConfig {
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Investment Choi API")
                        .version("2.0.0")
                        .description("주식 투자 수익 분석 및 자동 매매 시스템 API 문서")
                        .contact(new Contact()
                                .name("Investment Choi Team")
                                .email("support@investment-choi.com"))
                        .license(new License()
                                .name("Private")
                                .url("https://investment-choi.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://api.investment-choi.com")
                                .description("프로덕션 서버")));
    }
}
