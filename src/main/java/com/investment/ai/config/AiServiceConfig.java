package com.investment.ai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AI ?úÎπÑ???¥Îùº?¥Ïñ∏???§Ï†ï
 */
@Configuration
public class AiServiceConfig {
    
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
