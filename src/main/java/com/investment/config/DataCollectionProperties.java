package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 데이터 수집 API 설정 (DART, KRX, SEC EDGAR).
 * API 키는 로그 출력 시 LogMaskingUtil.maskApiKey 사용.
 */
@Component
@ConfigurationProperties(prefix = "investment.data")
@Getter
@Setter
public class DataCollectionProperties {

    private Dart dart = new Dart();
    private Krx krx = new Krx();
    private Sec sec = new Sec();
    /** 내부 수집 API 키 (X-Internal-Data-Key). 미설정 시 내부 API 비활성화 */
    private String internalApiKey = "";

    @Getter
    @Setter
    public static class Dart {
        /** Open DART API 인증키 (공공데이터포털/opendart.fss.or.kr 발급) */
        private String apiKey = "";
        private String baseUrl = "https://opendart.fss.or.kr/api";
        /** 수집 기간(일). 기본 3일 */
        private int collectDays = 3;
        /** 수집 cron. 기본 10분마다 */
        private String scheduleCron = "0 */10 * * * *";
    }

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
    public static class Sec {
        /** SEC EDGAR API 키 (data.sec.gov). 미설정 시 SEC 수집 스킵 */
        private String apiKey = "";
        private String baseUrl = "https://data.sec.gov";
        /** 수집 기간(일). 기본 3일 */
        private int collectDays = 3;
        /** 수집 cron. 기본 15분마다 */
        private String scheduleCron = "0 */15 * * * *";
    }
}
