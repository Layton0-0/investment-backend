package com.investment.alert;

/**
 * 긴급 알림 서비스 — 미체결·체결 실패 등 시 Discord 등으로 알림 발송.
 * 알림 본문에 userId, 계좌(마스킹 정책 적용), 모의/실계좌 여부, 증권사, URL 포함.
 */
public interface EmergencyAlertService {

        /**
         * 미체결 N분 경과 주문에 대한 긴급 알림 발송.
         *
         * @param orderId        주문 ID
         * @param symbol         종목
         * @param outstandingQty 미체결 수량 (API 미연동 시 주문 수량으로 대체 가능)
         * @param elapsedMin     경과 분
         * @param userId         사용자 ID
         * @param accountNo      계좌번호 (알림에는 마스킹 규칙 적용)
         * @param serverType     모의/실전 ("1"=모의, "0"=실전)
         * @param broker         증권사 (예: 한국투자증권)
         * @param baseUrl        앱 내 주문/계좌 화면 base URL (null 가능)
         */
        void sendUnfilledAlert(String orderId, String symbol, int outstandingQty, int elapsedMin,
                        String userId, String accountNo, String serverType, String broker, String baseUrl);

        /**
         * 체결 확인 후 포지션 등록 실패 등 일반 실패 알림 (선택).
         */
        void sendFailureAlert(String title, String message, String userId, String accountNo,
                        String serverType, String broker, String baseUrl);

        /**
         * 리스크 이벤트 알림 (일일 손실 한도 임박, VaR 초과 등).
         * Discord 발송 후 TB_ALERT_LOG에 저장.
         *
         * @param level     INFO, WARNING, ERROR
         * @param component 발신 컴포넌트 (예: DailyLossApproaching, VarExceeded)
         * @param message   알림 본문 (계좌·수치는 마스킹 후 포함)
         */
        void sendRiskEventAlert(String level, String component, String message);

        /**
         * Discord 웹훅 연결 테스트용 알림 1건 발송.
         *
         * @return Webhook URL이 설정되어 있고 발송에 성공하면 true, 미설정 또는 실패 시 false
         */
        boolean sendTestAlert();
}
