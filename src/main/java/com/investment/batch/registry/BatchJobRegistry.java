package com.investment.batch.registry;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 배치 Job 정의 레지스트리.
 * 스케줄러와 스케줄 현황 UI가 공통 참조.
 */
@Component
public class BatchJobRegistry {

        private static final String TZ = "Asia/Seoul";

        public List<BatchJobDefinition> getDefinitions() {
                return List.of(
                                BatchJobDefinition.builder()
                                                .id("trading-portfolio-generator")
                                                .name("트레이딩 포트폴리오 생성")
                                                .description("매일 한국 시간 오전 9시에 오늘의 트레이딩 포트폴리오를 자동 생성합니다. 실행: Backend 내부.")
                                                .cronExpression("0 0 9 * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trading-portfolios/generate")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("short-term-strategy-executor")
                                                .name("단기 전략 실행")
                                                .description("매 1시간마다 활성화된 단기 전략을 실행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 0 * * * *")
                                                .timeZone(TZ)
                                                .triggerPath(null)
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("medium-term-strategy-executor")
                                                .name("중기 전략 실행")
                                                .description("매일 오전 9시에 활성화된 중기 전략을 실행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 0 9 * * *")
                                                .timeZone(TZ)
                                                .triggerPath(null)
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("long-term-strategy-executor")
                                                .name("장기 전략 실행")
                                                .description("매주 월요일 오전 9시에 활성화된 장기 전략을 실행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 0 9 * * MON")
                                                .timeZone(TZ)
                                                .triggerPath(null)
                                                .build(),
                                // DART/SEC 공시 수집은 Python investment-data-collector에서 수행 (POST /dart-collect, /sec-collect)
                                BatchJobDefinition.builder()
                                                .id("krx-daily-collector")
                                                .name("KRX 일별 시세 수집")
                                                .description("매일 장 마감 후(16:00 KST) KRX 일별 시세를 수집합니다. 실행: Backend 내부(KrxCollectionService).")
                                                .cronExpression("0 0 16 * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/krx-daily")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("us-daily-collector")
                                                .name("US 시장 일별 시세 수집")
                                                .description("매일 미국 장 마감 후(17:00 KST) US 일별 시세를 수집합니다. 실행: Backend → data-collector POST /us-daily.")
                                                .cronExpression("0 0 17 * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/us-daily")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("krx-daily-backfill")
                                                .name("KRX 일별 시세 백필")
                                                .description("스트레스 구간 등 과거 기간 KRX 일별 시세 수집. from/to 파라미터 필수. 실행: Backend 내부(KrxCollectionService).")
                                                .cronExpression(null)
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/krx-daily-backfill")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("us-daily-backfill")
                                                .name("US 일별 시세 백필")
                                                .description("스트레스 구간 등 과거 기간 US 일별 시세 수집. from/to 파라미터 필수. 실행: Backend → data-collector.")
                                                .cronExpression(null)
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/us-daily-backfill")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("factor-calculation")
                                                .name("팩터 계산")
                                                .description("매일 장 시작 전(08:00 KST) 유니버스 필터 및 팩터(시그널) 계산을 실행합니다. 실행: Backend 내부(FactorCalculationService).")
                                                .cronExpression("0 0 8 * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/factor-calculation")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("auto-buy")
                                                .name("자동매수(통합)")
                                                .description("매일 09:10 KST에 공통 전처리 → 로보(ETF) → 파이프라인(개별종목) 순으로 통합 실행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 10 9 * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/auto-buy")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("auto-buy-us")
                                                .name("자동매수(미국장)")
                                                .description("매일 23:35 KST(미국 유리 시간대)에 파이프라인 US 시장만 실행합니다. 퀀트 유리 윈도우 내에서만 진입. 실행: Backend 내부.")
                                                .cronExpression("0 35 23 * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/auto-buy-us")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("pipeline-execution")
                                                .name("파이프라인 실행")
                                                .description("4단계 파이프라인만 수동 실행합니다. (스케줄은 자동매수(통합) 사용). 실행: Backend 내부.")
                                                .cronExpression(null)
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/pipeline-execution")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("pipeline-exit")
                                                .name("파이프라인 청산 평가")
                                                .description("장중 평일 5분마다 보유 포지션 청산 규칙을 평가하고 매도 시그널 시 주문 실행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 */5 9-15 * * MON-FRI")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/pipeline-exit")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("fill-confirmation")
                                                .name("체결 확인 후 포지션 등록")
                                                .description("매분 체결된 주문에 대해 포지션 등록을 수행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 * * * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/fill-confirmation")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("unfilled-order-check")
                                                .name("미체결 확인")
                                                .description("매분 PENDING N분 경과 주문에 대해 미체결 확인 및 알림을 수행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 * * * * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/unfilled-check")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("risk-event-alert")
                                                .name("리스크 이벤트 알림")
                                                .description("장중 평일 10분마다 일일 손실 한도 임박·VaR 95% 초과 검사 후 Discord 알림 발송. 실행: Backend 내부.")
                                                .cronExpression("0 */10 9-15 * * MON-FRI")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/risk-event-alert")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("medium-term-rebalance")
                                                .name("중기 리밸런스")
                                                .description("매월 1일 08:30 KST에 중기 전략 리밸런스를 실행합니다(스텁). 실행: Backend 내부.")
                                                .cronExpression("0 30 8 1 * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/medium-term-rebalance")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("daily-pnl")
                                                .name("일일 PnL 기록")
                                                .description("장 마감 후 평일 16:05 KST에 계좌별 당일 수익률을 기록합니다. 실행: Backend 내부.")
                                                .cronExpression("0 5 16 * * MON-FRI")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/daily-pnl")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("intraday-breakout")
                                                .name("장중 변동성 돌파")
                                                .description("평일 09:10, 09:40에 장중 변동성 돌파 진입 후보를 실행합니다. 실행: Backend 내부.")
                                                .cronExpression("0 10,40 9 * * MON-FRI")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/intraday-breakout")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("robo-rebalance")
                                                .name("로보 리밸런싱")
                                                .description("로보 어드바이저 리밸런싱만 수동 실행합니다. (스케줄은 자동매수(통합) 사용). 실행: Backend 내부.")
                                                .cronExpression(null)
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/robo-rebalance")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("strategy-governance-check")
                                                .name("전략 거버넌스 검사")
                                                .description("매월 1일 02:00 KST에 최근 N개월 백테스트 실행 후 MDD/Sharpe 열화 시 Discord 알림. 실행: Backend 내부.")
                                                .cronExpression("0 0 2 1 * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/strategy-governance-check")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("factor-decay-check")
                                                .name("팩터 열화 검사")
                                                .description("월 1회 팩터별 5일 수익률 Sharpe 미달 시 Discord 알림. 실행: Backend 내부.")
                                                .cronExpression("0 0 3 1 * *")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/factor-decay-check")
                                                .build(),
                                BatchJobDefinition.builder()
                                                .id("reconcile")
                                                .name("포지션 정합성(Reconciliation)")
                                                .description("장 시작 전 08:00 또는 장 마감 후 16:10에 자동투자 ON 계좌별 브로커-DB 포지션 비교, 불일치 시 Discord 알림. 실행: Backend 내부.")
                                                .cronExpression("0 0 8,16 * * MON-FRI")
                                                .timeZone(TZ)
                                                .triggerPath("/api/v1/trigger/reconcile")
                                                .build());
        }

        /**
         * 트리거 경로 → Job id 매핑 (예: /api/v1/trigger/dart-collect →
         * dart-disclosure-collector)
         */
        public String getJobIdByTriggerPath(String triggerPath) {
                if (triggerPath == null)
                        return null;
                return getDefinitions().stream()
                                .filter(d -> triggerPath.equals(d.getTriggerPath()))
                                .map(BatchJobDefinition::getId)
                                .findFirst()
                                .orElse(null);
        }

        public BatchJobDefinition getDefinition(String jobId) {
                return getDefinitions().stream()
                                .filter(d -> jobId.equals(d.getId()))
                                .findFirst()
                                .orElse(null);
        }
}
