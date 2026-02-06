현재 업로드해주신 `01-system-architecture.md` (Spring Boot 기반 모놀리식 구조)는 일반적인 웹 서비스로서는 훌륭하지만, **"헤지펀드급 퀀트 시스템"**으로 작동하기에는 **'의사결정의 깊이(Depth)'와 '데이터 정합성(Integrity)'** 측면에서 보강이 필요합니다.

기존의 **Spring Boot 구조를 최대한 유지**하면서, 기관 레벨의 **"Alpha(전략) - Risk(위험관리) - Execution(집행)"** 분리 원칙을 적용한 **개편안(Refactored Architecture)**을 제안합니다.

---

# 2.0 시스템 아키텍처 개편안 (Institutional-Grade)

핵심 변경 사항:

1. **Portfolio Construction Layer 추가:** 단순히 전략이 주문을 내는 것이 아니라, `Optimizer`가 세금/비용을 고려해 최적 비중을 계산합니다.
2. **Risk Management Layer 독립:** 주문 서비스(`OrderService`) 내부에 있던 검증 로직을 별도의 `ComplianceEngine`으로 분리하여 강력한 제동 장치를 마련합니다.
3. **Data Pipeline 강화:** 단순 조회(`MarketDataClient`)를 넘어, 수정주가(Adjusted Price) 처리 및 시계열 DB(TimescaleDB) 도입을 권장합니다.
4. **Hybrid ML Strategy:** 무거운 LLM 대신 가볍고 빠른 모델을 로컬에서 구동합니다.

---

## 1. 전체 논리 아키텍처 (Logical View)

기존의 `Controller -> Service -> Repository` 흐름을 **퀀트 엔진 파이프라인**으로 재해석합니다.

```mermaid
graph TD
    User[Client / Dashboard] --> WebLayer
    
    subgraph "Core System (Spring Boot)"
        WebLayer[API & Web Controller] --> Engine
        
        subgraph "Quant Engine"
            Data[Data Ingestion Service] --> |Clean & Adjust| FeatureStore[Feature & Factor Store]
            FeatureStore --> Alpha[Alpha Engine (Strategy)]
            Alpha --> |Raw Signals| Portfolio[Portfolio Optimizer]
            Portfolio --> |Target Weights| Risk[Risk Manager / Compliance]
            Risk --> |Approved Orders| Execution[Smart Order Router]
        end
        
        Execution --> |KIS API| Broker[Korea Investment & Securities]
    end
    
    subgraph "Intelligence Layer (Local/Python)"
        ML[ML Model Server / Script] -.-> |Inference via REST/DB| Alpha
    end

```

---

## 2. 패키지 및 컴포넌트 구조 재설계

기존 패키지 구조를 더 명확한 역할(Context) 중심으로 재편합니다.

```java
com.investment
├── api/ & web/              # (유지) Presentation Layer
├── core/
│   ├── engine/              # [NEW] 핵심 퀀트 엔진
│   │   ├── alpha/           # 전략 시그널 생성 (Momentum, MeanReversion)
│   │   ├── portfolio/       # [NEW] 포트폴리오 최적화 (세금/비용 계산)
│   │   ├── risk/            # [NEW] 리스크 관리 (Kill Switch, Exposure Limit)
│   │   └── execution/       # 주문 집행 및 스마트 라우팅
│   └── pipeline/            # 데이터 처리 파이프라인
├── domain/                  # (유지) JPA Entity
├── infra/
│   ├── kis/                 # (유지) 한투 API Wrapper
│   ├── ml/                  # [NEW] ML 모델 연동 (ONNX Runtime 등)
│   └── persistence/         # DB Repository
└── scheduler/               # 배치 작업 (Quartz or Spring Scheduler)

```

### 주요 신규/변경 컴포넌트 상세

#### A. `core.engine.portfolio` (두뇌 역할)

단순히 "매수 조건이면 산다"가 아니라, **"얼마나 사는 것이 세후 수익률(After-Tax)에 최적인가?"**를 계산합니다.

* **`TaxAwareOptimizer`**:
* 국내장: 거래세(0.20%) + 수수료 고려. 기대 수익이 비용을 넘지 않으면 `Weight = 0`.
* 해외장: 양도소득세(22%) 고려. 연말(12월)에는 손실 확정(Tax Harvesting) 로직 발동.


* **`Rebalancer`**: 현재 보유량(Current) vs 목표 비중(Target) 차이를 계산하여 매매 리스트 생성.

#### B. `core.engine.risk` (경찰 역할)

`OrderService`가 주문을 내기 직전에 반드시 통과해야 하는 관문(Gatekeeper)입니다.

* **`PreTradeCompliance`**:
* 개별 종목 비중 > 10% 시 차단.
* MDD(Maximum Drawdown) > 15% 도달 시 신규 매수 금지.
* **Hard Kill Switch**: 긴급 상황 시 DB 플래그 하나로 모든 주문 차단.



#### C. `infra.ml` (AI/ML 레이어)

이 부분은 아래 **[3. ML 전략 및 모델 분석]**에서 상세히 다룹니다.

---

## 3. ML(머신러닝) 모델 전략 및 분석

사용자님의 질문: **"기존 모델 중 로컬로 쓸 수 있는 것을 쓸 수 있는가?"**

### 결론부터 말씀드리면:

**가능합니다.** 하지만 거대 언어 모델(LLM, 예: Llama-3, GPT)을 주가 예측에 직접 쓰는 것은 **비추천**합니다. 대신, **"목적에 특화된 경량 모델"**을 로컬에 탑재하는 것이 훨씬 효율적이고 강력합니다.

### 추천하는 "로컬 구동" ML 전략 (Spring Boot + Python 연동)

헤지펀드에서 실제로 사용하는 방식 중, 개인 서버(Low Budget)에서 돌릴 수 있는 현실적인 방법입니다.

#### 옵션 1: 시계열/수치 분석 (추천: 직접 학습)

주가 데이터(OHLCV)를 기반으로 상승/하락을 예측하는 모델입니다. 남이 만든 모델은 내 데이터와 맞지 않습니다.

* **모델:** **XGBoost, LightGBM, Random Forest**
* **구동 방식:**
1. Python(PC)에서 과거 데이터로 학습 → `.model` 파일 생성 (Offline Training).
2. Spring Boot 내에 **`ONNX Runtime` (Java 라이브러리)** 을 탑재.
3. 서버에서는 Python 없이 Java가 `.model` 파일을 로드하여 **고속 추론(Inference)**만 수행.


* **장점:** 매우 빠름(ms 단위), CPU만으로 충분, 자바 환경과 완벽 통합.

#### 옵션 2: 뉴스/시황 분석 (추천: Pre-trained 활용)

뉴스가 "호재"인지 "악재"인지 판단합니다. 이는 직접 학습보다 잘 만들어진 모델을 쓰는 게 낫습니다.

* **모델:** **FinBERT** (금융 특화 BERT 모델)
* **소스:** Hugging Face (`ProsusAI/finbert` 등)
* **구동 방식:**
* Spring Boot 내부에서 돌리기엔 무거울 수 있음.
* **Docker Container (Python FastAPI)** 로 띄워서 Spring Boot가 REST API로 물어보는 구조.
* `Spring Boot (News Crawler) -> Python (FinBERT) -> Sentiment Score -> Spring Boot (Strategy)`


* **가능 여부:** 일반 VPS(RAM 8GB 이상)에서 CPU 모드로 충분히 돌아갑니다.

### 시스템 반영 계획

기존 `AnalysisService`를 **`MLSignalService`**로 업그레이드합니다.

1. **Regime Detection (시장 국면 판단):**
* (Python Offline) VIX, 이동평균선 등을 학습시켜 "변동성 장세" vs "추세 장세" 분류 모델 생성.
* (Spring Boot) 매일 아침 장 시작 전, 모델을 돌려 오늘의 **'Risk Factor'** 결정 (예: 위험하면 현금 비중 50% 강제).


2. **구현 로드맵:**
* 초기에는 ML 없이 `Technical Indicator`로 시작.
* 데이터가 쌓이면 로컬 PC에서 XGBoost 학습 후 모델 파일만 서버로 배포.



---

## 4. 인프라 및 데이터 구조 (Physical)

### 데이터베이스 마이그레이션 (권장)

현재 `MariaDB`를 쓰고 계시지만, 1분봉 데이터나 틱 데이터를 수년간 쌓으려면 **TimescaleDB (PostgreSQL 기반)**가 압도적으로 유리합니다.

* **대안:** MariaDB를 계속 쓰시겠다면, `History` 테이블에 반드시 **Partitioning**을 적용해야 합니다.

### 배치(Batch) 아키텍처

`Scheduler`를 단순 `@Scheduled` 어노테이션에서 **Spring Batch**로 업그레이드하는 것을 고려하세요. (실패 시 재시도, 로깅, 어디서 멈췄는지 추적 가능)

* `Job 1`: 시장 데이터 수집 (장 마감 후)
* `Job 2`: 기술적 지표 및 팩터 계산 (Feature Engineering)
* `Job 3`: 전략 시그널 생성 및 포트폴리오 최적화
* `Job 4`: 익일 매매 주문 생성 (Pre-Order)

---

## 5. 개발 로드맵 (Action Plan)

**Phase 1: Foundation (기반 다지기)**

* 기존 Spring Boot 코드를 위 패키지 구조(`core.engine.*`)로 리팩토링.
* `MarketDataClient`가 아닌 `DataPipeline` 구축 (수정주가 반영 로직 필수).
* 한투 API의 국내/해외 주문 로직 분기 처리 (`OrderService` 내).

**Phase 2: Risk & Portfolio (기관 로직 탑재)**

* `PreTradeCompliance` (주문 전 검수) 구현.
* 간단한 자산 배분 로직(예: 주식 60 / 채권 40) 구현.
* Web Dashboard에 "Kill Switch" 버튼 UI 추가.

**Phase 3: Intelligence (ML 도입)**

* Python 로컬 환경 세팅 (Jupyter Notebook).
* 데이터 추출(DB -> CSV) 후 간단한 XGBoost 모델 학습.
* ONNX Runtime을 통해 Spring Boot에서 모델 추론 연동.

이 방향으로 진행하시면 기존 코드를 버리지 않으면서도, 시스템의 격(Class)을 개인 투자자 수준에서 기관 수준으로 끌어올릴 수 있습니다. **어떤 부분부터 리팩토링 코드를 짜드릴까요?**