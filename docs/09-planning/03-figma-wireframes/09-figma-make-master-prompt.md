# Figma Make 마스터 프롬프트 (단일 입력용)

아래 내용을 **그대로** Figma Make 채팅에 한 번에 붙여 넣어서, 이 프로젝트의 전체 와이어프레임(페이지/프레임/컴포넌트)을 생성해줘.

---

## 0) 프로젝트 컨텍스트

- 제품: **Investment Choi** — 로보어드바이저형 자동투자 시스템(한국/미국 주식 통합)
- 핵심: **4단계 파이프라인**(유니버스 → 시그널 → 자금관리 → 매매실행) + (선택) 로보(ETF) 단계
- 전역 상태: **모의계좌(serverType=1) | 실계좌(serverType=0)** 탭을 모든 인증 후 화면에 표시하고 URL 쿼리로 유지
- 권한: **2역할**
  - `User`: 투자/설정/조회 중심
  - `Ops`: User + 운영(스케줄 트리거·데이터·알림·리스크·모델·감사·헬스)

---

## 1) 결과물(반드시 지켜)

### 1.1 스타일

- **로우-파이(저충실도) 와이어프레임**: 회색 톤, 단순한 도형/텍스트, 외부 이미지/아이콘 최소화(placeholder 가능)
- 데스크톱 기준: 본문은 **1200px 컨테이너** 중심 정렬
- 반응형 힌트만(선택): 모바일(1열) 프레임은 핵심 화면 2~3개만 추가해도 됨

### 1.2 구조(페이지/프레임)

- Figma 파일명: `Investment-Choi-Wireframes`
- 페이지(Page) 생성:
  - `00-IA-Sitemap`
  - `01-Auth`
  - `02-Dashboard`
  - `03-AutoInvest`
  - `04-Strategy`
  - `05-News`
  - `06-Portfolio`
  - `07-Orders`
  - `08-Batch`
  - `09-Backtest`
  - `10-Settings`
  - `11-Ops`
  - `12-Components`

- 프레임(Frame) 네이밍 규칙(엄수):
  - 형식: `screenId_state_desc`
  - 예: `dashboard_default_virtualAndReal`, `auto-invest_loading`, `settings_empty_noAccount`, `ops-alerts_default_list`
  - `state` 값 예: `default`, `loading`, `empty`, `error`, `hasAccount`, `noAccount`

### 1.3 레이아웃(모든 인증 후 화면 공통)

상단부터 아래 순서로 고정된 레이아웃을 사용해:
1) Header: 로고(대시보드 이동), 서비스명, 사용자명, 마이페이지, 로그아웃  
2) Menu: 세로 메뉴(아래 메뉴 트리 그대로) + 현재 메뉴 활성 표시  
3) AccountTabs: `모의계좌 | 실계좌` 전역 탭(선택 상태 표시)  
4) Container: 본문(카드/테이블/필터)

AutoLayout 규칙:
- 본문 Section은 세로 AutoLayout, 간격 16~24
- 카드 그리드는 2~4열(자동 줄바꿈), 테이블은 헤더 + 반복 행

---

## 2) 정보 구조(IA) / 메뉴 트리

### 2.1 User 메뉴(인증 후)

세로 메뉴 항목(표시명 그대로):
- 대시보드(`/`)
- 자동투자 현황(`/auto-invest`)
- 국내 전략(`/strategies/kr`)
- 미국 전략(`/strategies/us`)
- 뉴스·이벤트(`/news`)
- 포트폴리오(`/portfolio`)
- 주문·체결(`/orders`)
- 스케줄 현황(`/batch`)
- 백테스트(`/backtest`)
- 설정(`/settings`)

### 2.2 Ops 전용 메뉴(역할이 Ops일 때만 노출)

- 데이터 파이프라인 상태(`/ops/data`)
- 알림센터(`/ops/alerts`)
- 리스크 리포트(`/risk`)
- 모델/예측 상태(`/ops/model`)
- 감사 로그(`/ops/audit`)
- 시스템 헬스(`/ops/health`)

권한 규칙:
- `User`는 Ops 전용 메뉴를 **숨김**
- `Ops`는 전 메뉴 접근
- `/batch`의 “지금 실행” 버튼은 **Ops만 활성**(User는 비활성 또는 숨김)

---

## 3) 공통 컴포넌트(12-Components 페이지에 제작)

다음 컴포넌트를 만들고, 필요 화면에 재사용해:

### 3.1 기본 컴포넌트

- `Layout/Header`
- `Layout/Menu`
- `Layout/AccountTabs`
- `Layout/Container`

### 3.2 카드/지표

- `Card/Base` (제목+본문)
- `Card/Pipeline` (단계 라벨 + 요약 값)
- `Card/Summary` (계좌 요약)
- `Card/Stat` (라벨+값, positive/negative/neutral 변형)

### 3.3 테이블/필터/폼

- `Table/DataTable` (thead + row)
- `Filter/FilterBar` (기간/상태/시장 등)
- `Form/Input` (라벨/힌트/에러)
- `Form/SegmentControl` (예: 모의/실 선택)
- `Form/Toggle` (자동 매매, 로보 어드바이저)
- `Button/Primary`, `Button/Secondary`, `Button/Danger`

### 3.4 배지/상태(Variant)

- `Badge/Status`: `active`, `stopped`, `pending`, `executed`, `failed`
- `Toast`: success/error
- `State/Loading`(스켈레톤 or 스피너), `State/Empty`, `State/ErrorBox`

---

## 4) 가드레일(안전장치) UI 규칙(중요)

다음 조건을 화면에서 “명확한 문구 + 다음 액션 링크/버튼”으로 표현해:

- 계좌 미등록: “모의계좌를 등록해주세요” / “실계좌를 등록해주세요” + 설정 CTA
- 자동 매매 OFF: “주문이 나가지 않습니다. 설정에서 자동 매매를 켜세요.” + 설정 링크
- 서버 Dry-Run(auto-execute=false): “현재 서버는 Dry-Run 모드입니다. 실제 주문 실행은 서버 설정(PIPELINE_AUTO_EXECUTE)이 필요합니다.” (읽기 전용 안내)
- 실계좌 자동 실행 차단(allow-real-execution=false): “실계좌 자동 실행은 서버 설정(PIPELINE_ALLOW_REAL_EXECUTION)으로만 허용됩니다.” (Ops 화면에서 읽기 전용 표시)
- 리스크 게이트/일일 손실 한도: “신규 매수 축소/중단” 문구 + 원인 요약
- Rate limit: “요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.”

또한 실제 주문과 연결될 수 있는 행동은 확인 모달(2중 확인)을 사용해(예: 자동 매매 ON, 지금 실행).

---

## 5) 화면 생성 지시(각 페이지에 프레임 만들기)

각 화면은 최소 `default` 프레임을 만들고, 중요 화면은 `loading/empty/error`도 추가해.

### 5.1 01-Auth

- `auth-login_default` : 로그인 폼 + 회원가입 링크
- `auth-signup_default` : 회원가입 폼(서버 타입, API 키/시크릿, 계좌번호, 계좌 인증 버튼 포함)
- (선택) `auth-signup_error_verifyFailed`

### 5.2 02-Dashboard

- `dashboard_default_virtualAndReal`
  - 상단 QuickAction 카드 6개: 국내 전략, 미국 전략, 뉴스, 포트폴리오, 주문·체결, 설정
  - 아래에 “모의계좌 구역”과 “실계좌 구역”을 각각 카드/테이블로 구성
  - 각 구역 포함 요소:
    - 자동투자 상태 카드(자동 매매 ON/OFF, 유니버스 KR·US, 시그널 KR·US, 보유 포지션 수)
    - 계좌 요약 카드(국내/미국 보유 종목 수, 총 잔고)
    - Stat 카드 4개(총 자산, 보유 종목 수, 총 손익, 총 수익률)
    - 잔고 카드(총 잔고/가용 잔고)
    - 보유 종목 테이블(시장, 종목, 수량, 현재가, 평가금액, 손익)
    - 최근 주문 테이블(종목, 구분, 수량, 가격, 상태)

- `dashboard_empty_noAccount` : “계좌를 등록하여 투자를 시작하세요” + 설정 CTA

### 5.3 03-AutoInvest

- `auto-invest_default_summary`
  - 4단계 파이프라인 카드 4개:
    - 1 유니버스 KR·US
    - 2 시그널 KR·US
    - 3 자금 배분 요약(단기/중기/장기)
    - 4 보유 포지션 수
  - 시그널 테이블(시장, 종목, 팩터, 점수)
  - 보유 포지션 테이블(시장, 종목, 수량, 진입가, 진입일)
  - 가드레일 안내 영역(위 “가드레일 UI 규칙” 적용)

- `auto-invest_loading`
- `auto-invest_empty_noSignals`

### 5.4 04-Strategy

- `strategies-kr_default_list` : 단기/중기/장기 전략 카드/테이블(상태 배지 포함)
- `strategies-us_default_list` : 동일 구조
- (선택) `strategy_detail_default` : 전략 상세(파라미터/성과 요약)

### 5.5 05-News

- `news_default_list`
  - 상단 필터: 원천(Fact/Speed/Buzz), 시장, 기간, 종목
  - 목록 테이블: 제목, 원천, 시장, 종목, 수집 시각, (선택) 감정/중요도

### 5.6 06-Portfolio

- `portfolio_default`
  - 보유 종목 테이블
  - 트레이딩 포트폴리오: 날짜 선택 + 추천 종목 테이블(진입/손절/목표)

### 5.7 07-Orders

- `orders_default_list`
  - 필터 바: 기간, 상태, 시장
  - 주문 테이블: 주문일시, 종목, 시장, 매수/매도, 수량, 가격, 상태, 체결가, (미체결일 때만) 취소 버튼
- (선택) `orders_modal_cancelConfirm`

### 5.8 08-Batch

- `batch_default_jobs`
  - Job 목록 테이블: Job 이름, cron 설명, 마지막 실행, 성공/실패 횟수, 지금 실행 버튼(Opson만 활성)
  - “지금 실행”은 확인 모달(실제 주문 가능 시 경고 포함)

### 5.9 09-Backtest

- `backtest_default`
  - 상단 모드 선택: 4단계 | 로보
  - 4단계 폼: 기간·시장·전략·초기자본 + 실행 버튼
  - 결과 영역: 메트릭 카드, 수익 곡선 placeholder, 거래 목록 테이블
  - 로보 폼: 간단/고급 전환 + 고급 파라미터(모멘텀, MA, Top N, 리밸런싱, 수수료/슬리피지)

- `backtest_loading`

### 5.10 10-Settings

- `settings_default`
  - 상단: 계좌·API 연결(모의/실 세그먼트, API Key/Secret/계좌번호)
  - 하단: 자동투자 설정(최대 투자금액, 단/중/장 비율, 자동 매매 토글, 로보 토글, 저장 버튼)
  - 로보 ON 시 자동투자 ON으로 연동되는 안내 텍스트
  - 서버 가드(auto-execute/allow-real-execution)는 읽기 전용 안내로 표시

- `settings_empty_noAccount`

### 5.11 11-Ops

Ops 페이지에 다음 프레임을 만들고, **User에게는 숨김** 처리:

- `ops-data_default` : 원천별 수집 상태 카드/테이블(DART/SEC/KRX/US)
- `ops-alerts_default_list` : 알림 목록 + ACK 버튼 + 필터
- `risk_default_report` : 리스크 리포트(필터 + 차트 placeholder + 테이블)
- `ops-model_default` : 모델/예측 상태(health, model-ready, 버전, 실패율)
- `ops-audit_default` : 감사 로그 테이블
- `ops-health_default` : 시스템 헬스(외부 API/DB/Redis/Prediction 서비스 상태)

---

## 6) 샘플 데이터(화면에 채워)

아래 값은 예시로 채워:

- 시장: `KR`, `US`
- 종목: `005930`, `000660`, `AAPL`, `NVDA`, `TQQQ`
- 상태 배지: `ACTIVE`, `STOPPED`, `PENDING`, `EXECUTED`
- 손익/수익률: +1,230,000 / -540,000, +2.35% / -1.12%
- 파이프라인: 유니버스 `KR 120 · US 80`, 시그널 `KR 12 · US 7`, 보유 포지션 `4`

---

## 7) 금지/주의

- 하이파이 UI(과도한 색/이미지/아이콘)로 만들지 말 것. 와이어프레임에 집중.
- 실제 비밀번호/키/계좌번호 같은 민감 정보는 샘플 텍스트로만.
- `serverType` 전역 탭(모의/실)을 인증 후 모든 화면에 반드시 포함.

---

이제 위 요구사항대로 페이지/프레임/컴포넌트를 생성하고, 공통 컴포넌트는 `12-Components`에서 재사용 가능하도록 구성해줘.

