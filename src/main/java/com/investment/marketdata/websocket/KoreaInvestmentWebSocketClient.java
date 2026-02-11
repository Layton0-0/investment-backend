package com.investment.marketdata.websocket;

import java.util.List;

/**
 * 한국투자증권 실시간 WebSocket 클라이언트 인터페이스.
 * 실시간 호가(asking_price_total), 실시간 체결통보(ccnl_notice) 구독·해제·연결 관리.
 *
 * <p>MCP: 국내주식 실시간호가 통합(asking_price_total), 실시간체결통보(ccnl_notice)로
 * WebSocket URL·구독 포맷·TR_ID 확인 후 구현.
 *
 * @see com.investment.marketdata.websocket.NoOpKoreaInvestmentWebSocketClient
 */
public interface KoreaInvestmentWebSocketClient {

    /**
     * 실시간 호가 구독 (변동성 돌파 시그널 0.1초 단위 감시용).
     * MCP: asking_price_total.
     *
     * @param userId     API 키 소유 사용자 ID
     * @param serverType "1" 모의, "0" 실전
     * @param symbols    종목코드 목록 (6자리)
     */
    void subscribeQuote(String userId, String serverType, List<String> symbols);

    /**
     * 실시간 호가 구독 해제.
     */
    void unsubscribeQuote(List<String> symbols);

    /**
     * 실시간 체결통보 구독 (주문 체결 시 Push 수신 → 익절/손절 로직 트리거).
     * MCP: ccnl_notice.
     *
     * @param userId     API 키 소유 사용자 ID
     * @param serverType "1" 모의, "0" 실전
     */
    void subscribeCcnlNotice(String userId, String serverType);

    /**
     * 실시간 체결통보 구독 해제.
     */
    void unsubscribeCcnlNotice(String userId, String serverType);

    /**
     * 연결 (이용 순서: 연결 → 구독 등록 → 수신. 연결/종료 간격 최소 1초).
     */
    void connect(String userId, String serverType);

    /**
     * 연결 해제.
     */
    void disconnect(String userId, String serverType);

    /**
     * 현재 연결 여부.
     */
    boolean isConnected(String userId, String serverType);
}
