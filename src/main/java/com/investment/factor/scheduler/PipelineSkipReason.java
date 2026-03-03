package com.investment.factor.scheduler;

import lombok.Getter;

/**
 * 파이프라인 실행 스킵 사유 코드.
 * 로그·감사·알림에서 일관된 코드로 추적하기 위해 사용.
 */
@Getter
public enum PipelineSkipReason {

    NO_AUTO_TRADING_ACCOUNTS("NO_AUTO_TRADING_ACCOUNTS", "자동투자 ON 계좌 없음"),
    ACCOUNT_AUTO_EXECUTE_OFF("ACCOUNT_AUTO_EXECUTE_OFF", "계정별 자동 실행 OFF"),
    CAPITAL_NOT_SET("CAPITAL_NOT_SET", "자본 미설정"),
    RISK_GATE_NO_BUY("RISK_GATE_NO_BUY", "리스크 게이트 신규 매수 불가"),
    MARKET_CRASH_GATE("MARKET_CRASH_GATE", "시장 급락 게이트 신규 매수 중단"),
    DAILY_LOSS_LIMIT("DAILY_LOSS_LIMIT", "일일 손실 한도 초과"),
    GOVERNANCE_HALT("GOVERNANCE_HALT", "거버넌스 halt"),
    STRATEGY_STOPPED_OR_PAUSED("STRATEGY_STOPPED_OR_PAUSED", "전략 중지/일시정지"),
    RUN_FAILED("RUN_FAILED", "파이프라인 실행 실패"),
    OUTSIDE_TRADING_WINDOW("OUTSIDE_TRADING_WINDOW", "퀀트 매매 유리 시간대 밖(해당 시장 run 스킵)");

    private final String code;
    private final String description;

    PipelineSkipReason(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
