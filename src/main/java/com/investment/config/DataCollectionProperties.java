package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 데이터 수집 API 설정 (KRX, US). DART/SEC는 Python 수집기에서 수행·설정.
 * API 키는 로그 출력 시 LogMaskingUtil.maskApiKey 사용.
 */
@Component
@ConfigurationProperties(prefix = "investment.data")
@Getter
@Setter
public class DataCollectionProperties {

    private Krx krx = new Krx();
    private Us us = new Us();
    /** 내부 수집 API 키 (X-Internal-Data-Key). 미설정 시 내부 API 비활성화 */
    private String internalApiKey = "";

    @Getter
    @Setter
    public static class Krx {
        /** KRX Open API 인증키 (openapi.krx.co.kr 발급) */
        private String authKey = "";
        private String baseUrl = "https://openapi.krx.co.kr";
        /** 일별 시세 수집 cron. 기본 매일 16:00 KST (장 마감 후) */
        private String scheduleCron = "0 0 16 * * *";
    }

    @Getter
    @Setter
    public static class Us {
        /** US 시장 일별 시세 수집 cron. 기본 매일 17:00 KST (미국 장 마감 후) */
        private String scheduleCron = "0 0 17 * * *";
        /** yfinance 스크립트 절대/상대 경로. 미설정 시 US 일별 수집 스킵(스텁). collector-url 설정 시 무시 */
        private String yfinanceScriptPath = "";
        /**
         * Docker Compose us-daily-collector 서비스 URL (예: http://localhost:8001,
         * http://us-daily-collector:8001). 설정 시 스크립트 대신 HTTP 호출
         */
        private String collectorUrl = "";
        /** 수집 대상 종목 코드 (쉼표 구분). 스크립트에 --symbols 로 전달 */
        private String symbols = "AAPL,MSFT,GOOGL,AMZN,META,TSLA,NVDA,JPM,V,JNJ";
        /** Python 실행 명령 (예: python, python3, py). 기본 python. collector-url 사용 시 무시 */
        private String pythonCommand = "python";
        /** HTTP 호출 실패 시 재시도 횟수 (0이면 재시도 없음). 기본 2 */
        private int retryMax = 2;
        /** 재시도 대기 시간(ms). 지수 백오프의 초기값. 기본 1000 */
        private long retryInitialMs = 1000L;
        /** 수집 실패·0건 시 Discord 알림 발송 여부. 기본 false */
        private boolean failureAlertEnabled = false;
    }
}
