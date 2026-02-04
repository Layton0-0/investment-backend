package com.investment.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch 활성화.
 * JobRepository/JobLauncher 등은 Boot 자동 설정으로 등록됨.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
}
