# 기획·개발 기준 정리

**목적**: 앞으로의 개발은 아래 **세 기준 문서**에 따라 진행한다. 본 문서는 해당 문서의 역할 요약과 현재 프로젝트 구조와의 매핑을 한 곳에서 관리한다.

---

## 1. 기준 문서 지정

| 문서 | 역할 | 핵심 원칙 |
|------|------|-----------|
| [minimum-architecture-requirement.md](./minimum-architecture-requirement.md) | **최소 아키텍처 명세** | Asymmetric Sophistication(로직은 기관급, 인프라는 Fat VPS·Docker·Cron). KIS Adapter·Data Engine·Brain·Risk Guard·Tax. Phase 1~3 로드맵. |
| [기획요청.md](./기획요청.md) | **제품/시스템 기획 요청** | 전략·리스크·포트폴리오는 헤지펀드급, 인프라는 Low Budget. Web App·KIS·KR/US·연말 세금·Phase 1~4. |
| [gemini-설계.md](./gemini-설계.md) | **Spring Boot 개편안** | Alpha–Risk–Execution 분리, Portfolio Construction·Compliance 독립, Data Pipeline(수정주가·TimescaleDB), ML 경량(ONNX/FinBERT), `core.engine.*` 패키지·Phase 1~3. |

---

## 2. 기준 문서 요약

### 2.1 minimum-architecture-requirement.md

퀀트 트레이딩 시스템(QTS)을 **한국 개인 투자자**용으로, 기관급 로직과 저비용 인프라로 설계한다. **Asymmetric Sophistication**: 전략·리스크·포트폴리오는 헤지펀드 수준, 물리 인프라는 단일 VPS·Docker Compose·TimescaleDB·Cron 수준으로 유지한다. 백엔드 핵심 모듈은 Gateway(KIS Adapter), Data Engine(수집·수정주가·Feature Store), Brain(시그널·옵티마이저·리밸런서), Risk Guard(Kill Switch·비중 한도·MDD 게이트), Execution(스마트 라우팅)이다. 국내 거래세·해외 양도세를 반영한 Tax-Aware Alpha, ML은 오프라인 학습·경량 추론만 배포한다. Phase 1(Foundation) → Phase 2(Quant Engine) → Phase 3(Intelligence & Tax) 순으로 개발한다.

### 2.2 기획요청.md

**헤지펀드급 퀀트**가 **소규모 예산**으로 운영하는 시스템을 설계하라는 제품/시스템 기획 요청이다. 전략 품질·리스크 관리·포트폴리오 구성은 타협 없이 기관 수준으로, 인프라·ML 배포·운영 도구만 비용 제약을 둔다. 웹 기반 퀀트 대시보드, 백엔드 트레이딩 엔진, 전략·포트폴리오 엔진, 백테스트·시뮬레이션, 리스크 관리, 한국투자증권 API 주문 실행, 모니터링·로깅·리포팅을 포함한다. 논리 아키텍처(헤지펀드급)와 물리 인프라(저예산)를 명시적으로 분리하고, Phase 1~4(기반 → 안정화·리포팅 → ML 강화 → 선택적 확장) 로드맵을 제시한다. 연말 세금·리포트(국내/해외 실현 손익·배당·Hometax 대응)를 요구사항에 포함한다.

### 2.3 gemini-설계.md

기존 **Spring Boot 모놀리스를 유지**하면서, 기관급 **Alpha–Risk–Execution** 분리 원칙을 적용한 개편안이다. (1) Portfolio Construction Layer 추가: Optimizer가 세금/비용을 고려해 목표 비중 계산. (2) Risk Management Layer 독립: 주문 전 검증을 `ComplianceEngine`으로 분리(Kill Switch·비중 10%·MDD 15% 게이트). (3) Data Pipeline 강화: 수정주가 처리·TimescaleDB 도입. (4) Hybrid ML: LLM 대신 XGBoost/ONNX·FinBERT 등 경량 모델. 패키지 구조는 `core.engine.alpha`(시그널), `core.engine.portfolio`(TaxAwareOptimizer·Rebalancer), `core.engine.risk`(Compliance), `core.engine.execution`(스마트 라우팅), `core.pipeline`(데이터 파이프라인)으로 정리한다. Phase 1(Foundation·리팩토링·Data Pipeline) → Phase 2(Risk & Portfolio) → Phase 3(Intelligence·ML 도입) 순이다.

---

## 3. 논리 레이어 ↔ 현재 프로젝트 매핑

기준 문서의 논리 구조와 현재 코드베이스 패키지/서비스 대응 관계이다.

| 논리 레이어 | 기준 문서 용어 | 현재 프로젝트 (investment-backend) | 비고 |
|-------------|----------------|-------------------------------------|------|
| **Gateway** | KIS Adapter | `account`(KoreaInvestmentAccountClient), `order`(KoreaInvestmentOrderClient), `marketdata`(KoreaInvestmentMarketDataClient), 토큰·Rate Limiter·국내/해외 정규화 | 구현 완료. 문서 역할 보강만 필요. |
| **Data Engine** | Data Ingestion·Adjuster·Feature Store | `core.pipeline`, `datacollection`, `batch`(데이터 수집·팩터·유니버스·시그널) | 일부 완료. 수정주가·Feature Store 강화는 개발예정. |
| **Brain** | Strategy & Portfolio·Alpha·Optimizer·Rebalancer | `core.engine.alpha`(AlphaEngine), `core.engine.portfolio`(TaxAwareOptimizer, Rebalancer), `strategy`, `factor` | 대부분 구현됨. 전략 확장·세금 로직 고도화 진행예정. |
| **Risk Guard** | Compliance·Kill Switch·Exposure Limit | `core.engine.risk`(ComplianceEngine), `risk`(TradingHaltService, PortfolioPeakService) | 구현됨. VaR·연말 손실 한도 등 진행예정. |
| **Execution** | Smart Order Router | `order`(OrderService, executeOrderForPipeline), KR/US 분기 | 구현됨. 실전 Throttling·WebSocket은 장기. |

**인프라**: 단일 VPS·Docker Compose·TimescaleDB(PostgreSQL)·Cron/Spring Scheduler·GitHub Actions 배포 — [investment-infra](../investment-infra), [02-development-status.md](../09-planning/02-development-status.md)의 TimescaleDB 전환 완료와 정합.

---

## 4. 관련 문서와의 관계

- **[01-overview.md](./01-overview.md)**, **[PRD.md](../PRD.md)**  
  제품 개요·요구사항은 **본 기준 문서(세 문서)에 부합하도록 유지**한다. 기술 스택·Phase·용어는 본 문서 및 기준 문서와 일치시킨다.

- **[02-architecture/01-system-architecture.md](../02-architecture/01-system-architecture.md)**  
  시스템 아키텍처 문서는 위 논리 레이어(Gateway·Data Engine·Brain·Risk Guard·Execution)와 패키지 매핑을 반영하여, 기준 문서와의 대응 관계를 명시한다.

- **[09-planning/02-development-status.md](../09-planning/02-development-status.md)**, **[roadmap.md](../roadmap.md)**  
  개발 진행 현황·로드맵은 **기준 문서의 Phase(1~4) 및 레이어**에 맞춰 진행예정 항목을 그룹화·참조한다.

- **[09-planning/01-screen-menu-spec.md](../09-planning/01-screen-menu-spec.md)** (화면·메뉴 기획서), **[02-architecture/00-strategy-registry.md](../02-architecture/00-strategy-registry.md)** (전략 통합 레지스트리)  
  화면 기획·전략 수식·버전 스택은 **기준 문서에 부합하도록 유지**한다. 신규 기능·전략 변경 시 본 기준과 충돌하지 않도록 검토한다.

---

## 5. 문서 변경 이력

| 버전 | 일자 | 변경 내용 |
|------|------|-----------|
| 1.0 | 2026-02-06 | 초기 작성 — 기준 문서 3종 지정·요약·논리 레이어 매핑·관련 문서 참조 관계. |
