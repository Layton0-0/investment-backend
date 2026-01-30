package com.investment;

import com.investment.config.DotEnvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Investment Choi - AI 기반 고수익 자동매매 시스템
 * 
 * 주요 기능:
 * - AI 기반 가격 예측 및 투자 분석
 * - 강화학습 기반 전략 최적화
 * - 실시간 시장 데이터 처리
 * - 자동 매매 실행
 */
@SpringBootApplication
@EnableScheduling
@EnableCaching
public class InvestmentApplication {

    public static void main(String[] args) {
        DotEnvLoader.loadIfPresent();
        SpringApplication.run(InvestmentApplication.class, args);
    }
}
