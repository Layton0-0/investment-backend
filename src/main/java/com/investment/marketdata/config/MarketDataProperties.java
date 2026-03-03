package com.investment.marketdata.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 시장 데이터 API 설정 Properties
 */
@Component
@ConfigurationProperties(prefix = "investment.market-data")
@Getter
@Setter
public class MarketDataProperties {
    
    /**
     * 데이터 제공자 (korea-investment)
     */
    private String provider = "korea-investment";
    
    /**
     * 타임아웃 (ms)
     */
    private int timeout = 30000;
    
    /**
     * 모의 데이터 사용 여부
     */
    private boolean useMockData = false;
    
    /**
     * 한국투자증권 API 설정
     */
    private KoreaInvestmentProperties koreaInvestment = new KoreaInvestmentProperties();
    
    /**
     * 한국투자증권 API 설정
     */
    @Getter
    @Setter
    public static class KoreaInvestmentProperties {
        /**
         * App Key (한국투자증권 API 키)
         */
        private String appKey;
        
        /**
         * App Secret (한국투자증권 API 시크릿)
         */
        private String appSecret;
        
        /**
         * 서버 타입 (모의투자: "1", 실거래: "0")
         */
        private String serverType = "1"; // 기본: 모의투자
        
        /**
         * Access Token 캐시 사용 여부
         */
        private boolean useTokenCache = true;
        
        /**
         * 실전투자 여부 확인
         * @return 실전투자면 true, 모의투자면 false
         */
        public boolean isRealTrading() {
            return "0".equals(serverType);
        }

        /**
         * 토큰 갱신 설정 (장 시작 30분 전 자동 갱신)
         */
        private TokenRefreshProperties token = new TokenRefreshProperties();

        /**
         * 주문 API Throttling·큐 설정 (한국투자증권 실전 제한 준수)
         */
        private ThrottleProperties throttle = new ThrottleProperties();

        /**
         * 순위분석·투자자 매매동향 API (path·TR_ID는 MCP로 확인 후 설정)
         */
        private RankApiProperties rankApi = new RankApiProperties();

        /**
         * WebSocket 실시간 시세·체결통보 (미구현 시 enabled=false, NoOp 사용)
         */
        private WebSocketProperties websocket = new WebSocketProperties();

        @Getter
        @Setter
        public static class WebSocketProperties {
            /** WebSocket 사용 여부. true 시 실제 구현체 사용, false 시 NoOp */
            private boolean enabled = false;
            /** 실전/모의 Base URL (MCP asking_price_total, ccnl_notice 확인) */
            private String baseUrlReal = "wss://openapi.koreainvestment.com:9443";
            private String baseUrlVirtual = "wss://openapivts.koreainvestment.com:29443";
            /** WebSocket 경로 (예: /tryitout). 공식 문서·MCP 확인 */
            private String path = "/tryitout";
            /** 실시간 호가(통합) TR_ID. MCP asking_price_total (예: H0STASP0) */
            private String quoteTrId = "H0STASP0";
            /** 실시간 체결 TR_ID (예: H0STCNT0) */
            private String executionTrId = "H0STCNT0";
            /** 실시간 체결통보 TR_ID. MCP ccnl_notice (예: H0STCNI0) */
            private String ccnlNoticeTrId = "H0STCNI0";
            /** 연결 후 구독 전 최소 대기(ms). API 제한 준수(최소 1초) */
            private long connectWaitMs = 1000L;
            /** 구독 등록 간격(ms). 0.2초 이내 권장 */
            private long subscriptionIntervalMs = 200L;
            /** WebSocket 구독용 approval_key. REST로 발급받아 설정(선택). 미설정 시 approvalKeyFetchEnabled=true면 연결 시 REST 발급 시도 */
            private String approvalKey = "";
            /** approval_key 미설정 시 연결 시 REST /oauth2/Approval 호출로 발급 시도 여부 */
            private boolean approvalKeyFetchEnabled = false;
            /** 재연결 활성화 여부 */
            private boolean reconnectEnabled = true;
            /** 재연결 최대 시도 횟수 (0이면 무제한) */
            private int reconnectMaxAttempts = 5;
            /** 재연결 초기 대기 시간(ms) */
            private long reconnectInitialDelayMs = 1000L;
            /** 재연결 최대 대기 시간(ms) */
            private long reconnectMaxDelayMs = 60000L;
            /** 재연결 백오프 배수 */
            private double reconnectBackoffMultiplier = 2.0;
            /** PINGPONG 하트비트 간격(초). 0이면 비활성화 */
            private long heartbeatIntervalSeconds = 30L;
            /** 세션당 최대 구독 종목 수 (KIS 제한: 41개) */
            private int maxSubscriptionsPerSession = 41;
            /** 장 시작 전 WebSocket 연결·구독 크론 (비우면 기동 후 1회만 연결). 예: 0 0 8 * * MON-FRI (08:00 KST 평일) */
            private String connectCron = "";
        }

        @Getter
        @Setter
        public static class RankApiProperties {
            /** 거래량 순위 API path (미설정 시 getVolumeRank 빈 리스트 반환). MCP volume_rank 확인 */
            private String volumeRankPath = "";
            /** 거래량 순위 TR_ID (예: FHPST01710000). MCP 확인 */
            private String volumeRankTrId = "";
            /** 시장별 투자자 매매동향(일별) API path. MCP inquire_investor_daily_by_market 확인 */
            private String investorDailyPath = "";
            /** 시장별 투자자 매매동향 TR_ID. MCP 확인 */
            private String investorDailyTrId = "";
        }

        @Getter
        @Setter
        public static class TokenRefreshProperties {
            /** 장 시작 30분 전 토큰 갱신 사용 여부 */
            private boolean preMarketRefreshEnabled = true;
            /** 크론 표현식 (기본: 평일 08:30 KST). TaskScheduler에서 Asia/Seoul 적용 */
            private String preMarketRefreshCron = "0 30 8 * * MON-FRI";
        }

        @Getter
        @Setter
        public static class ThrottleProperties {
            /** 주문 큐·Throttling 사용 여부 */
            private boolean enabled = true;
            /** 주문 API 초당 허용 건수 (실전 약 2~10, 모의 2). 기본 5 */
            private int ordersPerSecond = 5;
            /** 주문 요청 큐 최대 길이. 초과 시 rejectWhenFull이면 거부 */
            private int queueMaxSize = 500;
            /** 큐 가득 찼을 때 요청 거부 여부. false면 블로킹 대기 */
            private boolean rejectWhenFull = true;
        }
    }
}
