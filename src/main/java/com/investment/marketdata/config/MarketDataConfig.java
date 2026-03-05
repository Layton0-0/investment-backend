package com.investment.marketdata.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * 시장 데이터 API 설정
 * 
 * 한국투자증권 API는 2025.12.12(금)부터 TLS 1.0, 1.1 지원을 중단하고
 * TLS 1.2 이상만 지원합니다.
 * 
 * Java 17을 사용하는 경우 기본적으로 TLS 1.2, 1.3을 지원하므로
 * 별도 설정이 필요 없지만, 명시적으로 TLS 버전을 제한합니다.
 */
@Slf4j
@Configuration
public class MarketDataConfig {
    
    /**
     * WebClient 빈 생성
     * TLS 1.2 이상만 사용하도록 설정
     */
    @Bean
    public WebClient webClient() {
        try {
            // TLS 1.2, 1.3만 허용하는 SSL Context 생성
            SslContext sslContext = SslContextBuilder.forClient()
                    .protocols("TLSv1.2", "TLSv1.3") // TLS 1.2, 1.3만 허용
                    .build();
            
            // HttpClient에 SSL Context 설정
            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
            
            log.info("WebClient 설정 완료: TLS 1.2, 1.3 지원");
            
            return WebClient.builder()
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();
        } catch (SSLException e) {
            log.warn("SSL Context 설정 실패, 기본 설정 사용: {}", e.getMessage());
            // SSL 설정 실패 시 기본 WebClient 반환 (Java 17은 기본적으로 TLS 1.2 이상 지원)
            return WebClient.builder()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                    .build();
        }
    }
    
    @Bean
    public ObjectMapper objectMapper(org.springframework.http.converter.json.Jackson2ObjectMapperBuilder builder) {
        return builder.build();
    }
}
