# 프로젝트 로드맵

## 현재 상태

- **현재 버전**: 1.0.0
- **현재 단계**: 개발 중
- **시작일**: 2026년 1월
- **마지막 업데이트**: 2026년 2월 11일
- **비전**: **자동투자수익** — 사람 개입 없이 퀀트 엔진 기반 자동 매매 (MDD -15% 이내, CAGR 30%+ 목표). 국내(KR)·미국(US) 4단계 파이프라인(유니버스→시그널→자금관리→매매실행), 확정 공시/뉴스/센티멘트 원천만 파이프라인 연결. [자동투자 전략 명세](./02-architecture/12-auto-investment-strategy.md), [뉴스·공시 수집·연동 설계](./02-architecture/13-news-collection-design.md) 참조.
- **진행 현황**: 완료·진행중·진행예정 목록은 **[개발 진행 현황](./09-planning/02-development-status.md)** 문서를 참조하고, 변동 시 해당 문서를 수정한다.
- **기준 문서**: 앞으로의 개발은 [기획·개발 기준 정리](./01-requirements/00-planning-basis.md)에서 지정한 세 문서(minimum-architecture-requirement, 기획요청, gemini-설계)에 따라 진행한다. Phase 1(Foundation) → Phase 2(Quant Engine & Risk) → Phase 3(Intelligence & Tax) → Phase 4(Stability & Optional Scale)와 본 로드맵의 단기·중기·장기 Phase는 [02-development-status.md](./09-planning/02-development-status.md)에서 레이어별로 정합되어 있다.

## 단기 로드맵 (1-3개월)

### Phase 1: 기반 인프라 구축 (1-2주)

**목표**: 안정적인 인프라 기반 구축 및 핵심 기능 안정화

#### 1.1 Redis 캐싱 레이어 구현
- [ ] Redis 설정 및 연결 테스트
- [ ] `@Cacheable` 어노테이션 적용
  - 시장 데이터 캐싱 (5분 TTL)
  - 종목 분석 결과 캐싱 (10분 TTL)
  - 계좌 정보 캐싱 (1분 TTL)
- [ ] 캐시 무효화 전략 구현

**예상 완료일**: 2026년 2월 초

#### 1.2 OpenAPI/Swagger 문서화
- [ ] SpringDoc 설정
- [ ] API 엔드포인트 문서화
- [ ] DTO 스키마 정의
- [ ] 예제 요청/응답 추가

**예상 완료일**: 2026년 2월 초

#### 1.3 Circuit Breaker 패턴 적용
- [ ] Resilience4j 설정
- [ ] 외부 API 호출에 Circuit Breaker 적용
  - 시장 데이터 API (한국투자증권)
  - AI 예측 서비스
- [ ] Fallback 전략 구현

**예상 완료일**: 2026년 2월 중순

### Phase 2: AI 서비스 통합 (2-3주)

**목표**: AI 기반 예측 서비스 통합 및 기본 모델 구현

#### 2.1 AI 서비스 클라이언트 완성
- [ ] `AiPredictionClient` 인터페이스 구현
- [ ] `FastApiPredictionClient` 완성
- [ ] 재시도 로직 구현
- [ ] 타임아웃 처리

**예상 완료일**: 2026년 2월 중순

#### 2.2 Python FastAPI 서비스 기본 구조
- [ ] `ai-service/prediction-service/` 디렉토리 생성
- [ ] FastAPI 기본 구조 구현
- [ ] Health check 엔드포인트
- [ ] 간단한 예측 API (Mock 데이터)
- [ ] Dockerfile 작성 (선택)

**예상 완료일**: 2026년 2월 말

#### 2.3 LSTM 예측 모델 (초기 버전)
- [ ] 데이터 수집 및 전처리
- [ ] 간단한 LSTM 모델 개발
- [ ] 모델 학습 파이프라인
- [ ] 모델 서빙 API 구현

**예상 완료일**: 2026년 3월 초

### Phase 3: 핵심 기능 강화 (2-3주)

**목표**: 핵심 기능의 안정성 및 성능 향상

#### 3.1 시장 데이터 처리 개선
- [ ] Redis 캐싱 적용
- [ ] Circuit Breaker 적용
- [ ] 에러 핸들링 강화

**예상 완료일**: 2026년 3월 중순

#### 3.2 종목 분석 서비스 개선
- [ ] AI 예측 결과 통합
- [ ] 기술적 지표 계산 최적화
- [ ] 분석 결과 캐싱

**예상 완료일**: 2026년 3월 중순

#### 3.3 전략 실행 엔진 개선
- [ ] 실시간 데이터 기반 의사결정
- [ ] 리스크 관리 강화
- [ ] 로깅 및 모니터링

**예상 완료일**: 2026년 3월 말

### Phase 4: 테스트 및 안정화 (1-2주)

**목표**: 코드 품질 향상 및 안정성 확보

#### 4.1 테스트 커버리지 향상
- [x] 단위 테스트 작성 (목표: 80% 라인 커버리지)
- [x] API 테스트 작성 (슬라이스·컨트롤러 테스트)
- [ ] 통합 테스트 작성 (선택)

**예상 완료일**: 2026년 4월 초

#### 4.2 성능 최적화
- [x] 대시보드 병렬 로딩 (잔고·보유·주문·설정)
- [x] 현재가 동기 캐시·다중 종목 병렬 조회
- [ ] 쿼리 최적화 (선택)
- [ ] 캐싱 전략 추가 최적화 (선택)
- [ ] 비동기 처리 확대 (선택)

**예상 완료일**: 2026년 4월 중순

## 중기 로드맵 (3-6개월)

### Phase 5: 고급 기능 개발 (2-3개월)

**목표**: 고급 분석 기능 및 전략 확장. 자동투자 4단계 파이프라인·시장(KR/US)별 알고리즘·뉴스 파이프라인 반영.

#### 5.1 고급 분석 기능
- [x] 섹터 분석 기능 — GET /api/v1/analysis/sector·포트폴리오 섹터 비중·수익 기여도
- [x] 상관관계 분석 — GET /api/v1/analysis/correlation·리스크 기반 포지션 사이징
- [x] 리스크 메트릭 강화 — VaR/CVaR·포트폴리오 리스크 메트릭·리밸런싱 제안 API

**예상 완료일**: 2026년 6월

#### 5.2 전략 확장 (시장·기간별)
- [x] **수정주가(Adjuster) 파이프라인 — Phase 2 Quant Engine 필수** — 일봉 저장·팩터 계산·백테스트 입력은 수정주가만 사용. 원주가는 차트 표시 등에만 사용. 상세: [개발 진행 현황](09-planning/02-development-status.md), [decisions.md](decisions.md) 데이터 정합성 ADR.
- [ ] **백테스트 스트레스 검증** — 시나리오·검증 기준·실행 방법은 [backtest-stress-results.md](02-architecture/backtest-stress-results.md)에 문서화 완료. **실행 결과 기입은 해당 구간(2020-02~04·2022-01~06) 데이터 수집 후** 동 문서 §3에 기입 시 본 항목 완료([x] 처리). [개발 진행 현황](09-planning/02-development-status.md) 진행예정 참조.
- [x] **Walk-Forward 또는 Train/Test 기간 분리 백테스트** (권장) — WalkForwardBacktestService·POST /api/v1/backtest/walk-forward 구현 완료.
- [x] 팩터 계산 엔진 (구축 로드맵 2단계) — KRX 일별 저장·이격도·변동성 돌파·유동성·GET /api/v1/signals·자동투자 현황 시그널 연동
- [x] 4단계 파이프라인 (1차) — 유니버스(유동성)·시그널 유니버스 필터·PositionSizingService·PipelineExecutor·ExitRuleService(Time-Cut). 상세: [개발 진행 현황](09-planning/02-development-status.md)
- [x] 시장(Market KR/US) 차원 도입 — 전략·유니버스·시그널 시장별 분리·PipelineSummary·기간별 청산·자금관리
- [x] 중기 전략 (MEDIUM_TERM) 구현 — -10% 손절·Time-Cut·시그널 상위 10% 등 (상세: 개발 진행 현황)
- [ ] 장기 전략 (LONG_TERM) 구현 — 현재 스텁, 후속 구현
- [x] 4단계 파이프라인 확장 (1차) — 변동성 돌파 k 동적 적용, Half-Kelly 자금 관리, ATR Trailing Stop 청산, 체결 확인 후 포지션 등록 옵션, 유니버스/시그널 필터 스텁 구현. 상세: [개발 진행 현황](09-planning/02-development-status.md)
- [x] 4단계 파이프라인 확장 (데이터 수집 후) — Sector RS·Post-Earnings Drift·수급 강도·듀얼 모멘텀·퀄리티-성장 실제 계산, Half-Kelly 백테스트 연동. 상세: [개발 진행 현황](09-planning/02-development-status.md)
- [ ] 커스텀 전략 생성 기능

**예상 완료일**: 2026년 7월

#### 5.3 뉴스·공시 파이프라인
- [ ] 확정 원천만 파이프라인 연결 (DART·연합·네이버 / SEC·Reuters·Yahoo)
- [ ] 뉴스·공시 수집·저장·감정/중요도 분석
- [ ] 전략 연동 (시그널 점수 반영, 8-K/DART 최우선 처리)

**예상 완료일**: 2026년 7월

#### 5.3 포트폴리오 최적화
- [ ] 포트폴리오 리밸런싱 자동화
- [ ] 리스크 기반 포지션 사이징
- [x] 백테스팅 기능 — POST /api/v1/backtest, /backtest 화면, ExitRuleEvaluator·BacktestService, MDD/CAGR/Sharpe/Sortino/Calmar·승률·손익비 노출. Half-Kelly p·b 실전 주입은 별도.
- [x] **모의계좌 자동투자 실행** — 스케줄러 매수 시 executeOrderForPipeline·userId(TradingSetting 조회), 실행 대상 계좌 거래설정(TradingSetting) 기준. 실계좌 전 **모의 2주 테스트** 권장.

**예상 완료일**: 2026년 7월

### Phase 6: 사용자 경험 개선 (1-2개월)

**목표**: 웹 인터페이스 개선 및 사용성 향상

#### 6.1 대시보드 개선
- [ ] 실시간 차트 통합
- [x] 성과 분석 대시보드 — 성과 요약 섹션·총 평가액·MDD·Sharpe·Sortino·VaR 95%·CVaR 95% 카드 표시 (GET /api/v1/dashboard/performance-summary 연동 완료). 실시간 차트는 대시보드 가격 추이(PriceChart)로 1종목 표시됨.
- [ ] 알림 기능 (앱 내 사용자 알림)

**예상 완료일**: 2026년 8월

#### 6.2 모바일 반응형 지원
- [ ] 반응형 웹 디자인
- [ ] 모바일 최적화

**예상 완료일**: 2026년 8월

## 장기 로드맵 (6개월 이상)

### Phase 7: 확장 기능 (6개월+)

**목표**: 시스템 확장 및 새로운 기능 추가

#### 7.1 다중 계좌 지원
- [ ] 다중 계좌 동시 관리
- [ ] 계좌 간 자금 이체
- [ ] 통합 포트폴리오 뷰

**예상 완료일**: 2026년 하반기

#### 7.2 실시간 스트리밍
- [ ] WebSocket 기반 실시간 시세 스트리밍
- [ ] 실시간 알림 시스템

**예상 완료일**: 2026년 하반기

#### 7.3 국내·미국 통합 (한투 KIS API)
- [ ] 한국투자증권 KIS Open API로 국내·미국 주식 단일 인터페이스
- [ ] WebSocket 우선 시세·체결, REST 퀀트 스코어링(순위분석·매매동향·해외주식 시세)
- [ ] 통합 증거금·Throttling·토큰 갱신(Crontab)

**예상 완료일**: 2026년 하반기

#### 7.4 해외 주식 추가 시장 (선택)
- [ ] 추가 시장 거래 API 연동
- [ ] 환율 처리
- [ ] 글로벌 포트폴리오 관리

**예상 완료일**: 2027년 상반기

### Phase 8: 모바일 앱 개발 (향후)

**목표**: 네이티브 모바일 앱 제공

- [ ] iOS 앱 개발
- [ ] Android 앱 개발
- [ ] 푸시 알림

**예상 완료일**: 2027년

## 마일스톤 및 릴리스 계획

### v1.0.0 (현재)
- ✅ 기본 계좌 관리 기능
- ✅ 주문 실행 및 관리
- ✅ 종목 분석 (기술적 지표)
- ✅ 전략 관리 기본 기능
- ✅ 웹 대시보드

**목표 완료일**: 2026년 4월

### v1.1.0
- Redis 캐싱 레이어
- OpenAPI/Swagger 문서화
- Circuit Breaker 패턴 적용

**목표 완료일**: 2026년 5월

### v1.2.0
- AI 예측 서비스 통합
- LSTM 모델 기본 구현
- 고급 분석 기능

**목표 완료일**: 2026년 7월

### v2.0.0
- 다중 계좌 지원
- 실시간 스트리밍
- 고급 전략 기능

**목표 완료일**: 2026년 하반기

## 우선순위 매트릭스

### 높은 우선순위 (P0)
- Redis 캐싱 레이어 구현
- OpenAPI/Swagger 문서화
- Circuit Breaker 패턴 적용
- 테스트 커버리지 향상

### 중간 우선순위 (P1)
- AI 서비스 통합
- LSTM 모델 구현
- 성능 최적화
- 대시보드 개선

### 낮은 우선순위 (P2)
- 다중 계좌 지원
- 실시간 스트리밍
- 해외 주식 지원
- 모바일 앱 개발

## 리스크 및 대응 방안

### 기술적 리스크
- **한국투자증권 API 제약**: Rate Limiting 및 Circuit Breaker로 대응
- **AI 모델 성능**: 단계적 개선 및 Fallback 전략 수립

### 일정 리스크
- **개발 지연**: 우선순위 조정 및 범위 조정
- **의존성 문제**: 대안 기술 스택 검토

### 비즈니스 리스크
- **규제 변경**: 금융 규정 모니터링 및 대응
- **시장 변화**: 유연한 전략 조정 기능 제공

## 참고 문서

- **[기획·개발 기준 정리](./01-requirements/00-planning-basis.md)** — 개발 기준(세 문서 요약·논리 레이어 매핑)
- [minimum-architecture-requirement.md](./01-requirements/minimum-architecture-requirement.md) — 최소 아키텍처 명세 (QTS·Fat VPS·Phase 1~3)
- [기획요청.md](./01-requirements/기획요청.md) — 제품/시스템 기획 요청 (헤지펀드급 로직·Phase 1~4)
- [gemini-설계.md](./01-requirements/gemini-설계.md) — Spring Boot 개편안 (Alpha–Risk–Execution·core.engine)
- [PRD](./PRD.md)
- **[개발 진행 현황](./09-planning/02-development-status.md)** — 완료·진행중·진행예정 목록 (참조·변동 시 수정)
- [아키텍처 결정 사항](./decisions.md)
- [자동투자 전략 명세](./02-architecture/12-auto-investment-strategy.md) — 4단계 파이프라인·시장별 알고리즘·KIS 실전 구축
- [뉴스·공시 수집·연동 설계](./02-architecture/13-news-collection-design.md) — 확정 원천·전략 연동
- [화면·메뉴 기획서](./09-planning/01-screen-menu-spec.md) — 확장 가능 메뉴·화면
- [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md)
- [기능 요구사항](./01-requirements/02-functional-requirements.md)

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-29 | System | 초기 로드맵 작성 - essential-tech-spec.md의 Phase 정보 기반 |
| 1.1 | 2026-01-29 | System | 자동투자수익 비전·4단계 파이프라인·국내/미국·뉴스 파이프라인·KIS API 단계 반영 |
| 1.2 | 2026-02-06 | System | 기준 문서(00-planning-basis·minimum·기획요청·gemini) 참조 추가, Phase 기준 문서 정합 문구 반영 |
| 1.3 | 2026-02-11 | System | 기획 고도화(퀀트 관점): Phase 5.2 수정주가 필수·스트레스 검증(2020/2022)·Walk-Forward(권장) 체크 추가 |
| 1.4 | 2026-02-19 | System | 문서 동기화: Phase 5.1·5.2 완료 항목 [x] 반영(수정주가·Walk-Forward·시장 KR/US·중기 전략·4단계 확장 데이터 수집 후·고급 분석). 백테스트 스트레스 검증 문구 정리(결과 기입 완료 시 [x] 처리). |
