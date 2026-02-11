package com.investment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 전략 거버넌스 자동화 설정.
 * 정기 백테스트 실행·열화 기준·알림(1차는 알림만, 2차에서 자동 중단 선택).
 */
@Component
@ConfigurationProperties(prefix = "investment.governance")
@Getter
@Setter
public class GovernanceProperties {

    /** 정기 검토 사용 여부. false면 스케줄 미등록, 수동 트리거만 가능 */
    private boolean enabled = true;

    /** 최근 N개월 구간으로 백테스트 실행 (기본 12) */
    private int lookbackMonths = 12;

    /** MDD 열화 기준 (%). 이 값보다 낮으면 열화. 예: -15 → MDD -20%면 열화 (기본 -15) */
    private BigDecimal mddThresholdPct = new BigDecimal("-15");

    /** Sharpe 최소 기준. 이 값 미만이면 열화 (기본 0) */
    private BigDecimal sharpeMin = BigDecimal.ZERO;

    /** 백테스트 기본 자본금 (원). 정기 검토 시 사용 */
    private BigDecimal defaultCapital = new BigDecimal("100000000");

    /** 1차: true면 알림만 발송. false면 알림 + (2차 자동 매매 중단 연동) */
    private boolean alertOnly = true;

    /** 2차: 열화 시 (market, strategyType)별 halt 등록 여부. true이고 alertOnly=false일 때만 halt 등록 (기본 false) */
    private boolean autoHaltOnDegradation = false;
}
