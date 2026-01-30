# Investment Choi 문서

## 문서 구조

이 문서는 Investment Choi 프로젝트의 기획 및 설계 문서를 포함합니다.

## 기획 문서 (최상위)

프로젝트의 핵심 기획 문서입니다. Cursor MCP를 활용하여 자동으로 관리 및 업데이트할 수 있습니다.

- **[PRD (Product Requirements Document)](./PRD.md)** - 제품 요구사항 통합 문서
- **[로드맵](./roadmap.md)** - 프로젝트 개발 계획 및 마일스톤
- **[개발 진행 현황](./09-planning/02-development-status.md)** - 완료·진행중·진행예정 목록 (개발 시 참조·변동 시 수정)
- **[아키텍처 결정 사항](./decisions.md)** - 주요 기술 및 설계 결정 기록 (ADR)

## 문서 목록

### 1. 요구사항 (01-requirements) ✅

- [프로젝트 개요](./01-requirements/01-overview.md)
- [기능 요구사항](./01-requirements/02-functional-requirements.md)
- [비기능 요구사항](./01-requirements/03-non-functional-requirements.md)

### 2. 아키텍처 (02-architecture)

#### 현재 사용 중 ✅
- [시스템 아키텍처](./02-architecture/01-system-architecture.md) ✅
- [설계 패턴 및 원칙](./02-architecture/02-design-patterns.md) ✅
- [자동투자 전략 명세](./02-architecture/12-auto-investment-strategy.md) ✅ — 4단계 파이프라인·시장별 알고리즘·KIS 실전 구축
- [뉴스·공시 수집·연동 설계](./02-architecture/13-news-collection-design.md) ✅ — 확정 원천·전략 연동
- [필수 기술 스펙](./02-architecture/10-essential-tech-spec.md) ✅

#### 참고용 문서 📚
- [정량적 전략](./02-architecture/05-quantitative-strategy.md) 📚
- [성능 영향 분석](./02-architecture/06-performance-impact-analysis.md) 📚
- [비용 최적화 아키텍처](./02-architecture/09-cost-optimized-architecture.md) 📚
- [시장 데이터 리팩토링](./02-architecture/11-market-data-refactoring.md) 📚

#### 과거 설계 문서 (참고용) 📜
- [AI 재설계](./02-architecture/03-ai-redesign.md) 📜
- [구현 계획](./02-architecture/04-implementation-plan.md) 📜
- [프론트엔드 간소화](./02-architecture/07-frontend-simplification.md) 📜
- [프론트엔드 아키텍처](./02-architecture/08-frontend-architecture.md) 📜 (확장 가능 메뉴·09-planning 참조 반영)

### 3. 설계 (03-design) ✅

- [도메인 모델 설계](./03-design/01-domain-model.md) ✅

### 3.5 기획 (09-planning) ✅

- [화면·메뉴 기획서](./09-planning/01-screen-menu-spec.md) ✅ — 확장 가능 메뉴·메뉴별 화면·확장 규칙
- **[개발 진행 현황](./09-planning/02-development-status.md)** — 완료·진행중·진행예정 목록 (참조·변동 시 수정)

### 4. API (04-api) ✅

- [API 개요](./04-api/01-api-overview.md) ✅
- [API 엔드포인트 상세](./04-api/02-api-endpoints.md) ✅ (계좌 API 포함)
- [주문 API](./04-api/03-order-api.md) ✅
- [전략 API](./04-api/04-strategy-api.md) ✅
- [분석 API](./04-api/05-analysis-api.md) ✅
- [설정 API](./04-api/06-setting-api.md) ✅
- [트레이딩 포트폴리오 API](./04-api/07-trading-portfolio-api.md) ✅
- [키움증권 API 가이드](./04-api/08-kiwoom-api-guide.md) ✅
- [한국투자증권 API 가이드](./04-api/09-korea-investment-api-guide.md) ✅

### 5. 데이터베이스 (05-database) ✅

- [데이터베이스 스키마 설계](./05-database/01-database-schema.md) ✅

### 6. 배포 및 운영 (06-deployment) ✅

- [배포 가이드](./06-deployment/01-deployment-guide.md) ✅
- [운영 가이드](./06-deployment/02-operations-guide.md) ✅
- [서버 스펙](./06-deployment/03-server-specification.md) ✅
- [최소 비용 구성](./06-deployment/04-minimal-cost-setup.md) ✅ (비용 최적화 내용 통합)

### 7. 보안 (07-security) ✅

- [보안 리팩토링 가이드](./07-security/01-security-refactoring-guide.md) ✅
- [보안 설정 참조 가이드](./07-security/02-security-configuration-reference.md) ✅

### 8. 일시적 설치 가이드 (08-setup-guides) 🔧

로컬 개발 환경 구축 및 마이그레이션을 위한 일시적 가이드입니다.

- [Windows 로컬 개발 환경 구축](./08-setup-guides/01-local-windows-setup.md) 🔧
- [로컬 Docker 인프라 설정](./08-setup-guides/02-local-docker-setup.md) 🔧
- [local-maria 설정](./08-setup-guides/03-local-maria-setup.md) 🔧
- [Windows MariaDB → Docker 마이그레이션](./08-setup-guides/04-mariadb-migration-to-docker.md) 🔧
- [인코딩 문제 해결](./08-setup-guides/05-encoding-fix-guide.md) 🔧
- [한국투자증권 MCP 통합 가이드](./08-setup-guides/06-mcp-integration-guide.md) ✅
- [MCP 기획 문서 관리 가이드](./08-setup-guides/07-mcp-planning-guide.md) ✅

## 문서 상태 표시

- ✅ **현재 사용 중**: 프로젝트에서 현재 사용 중인 문서
- 📚 **참고용**: 참고 자료로 활용하는 문서
- 📜 **과거 설계**: 과거 설계 문서 (참고용)
- 🔧 **일시적 설치 가이드**: 로컬 개발 환경 구축 및 마이그레이션 가이드

## 문서 작성 규칙

1. **마크다운 형식**: 모든 문서는 Markdown 형식으로 작성
2. **한국어 사용**: 모든 문서는 한국어로 작성
3. **버전 관리**: 문서 변경 시 버전 이력 관리
4. **정기 업데이트**: 코드 변경 시 관련 문서도 함께 업데이트
5. **개발 진행 현황**: 개발 완료·진행중·진행예정이 바뀔 때마다 [09-planning/02-development-status.md](./09-planning/02-development-status.md)를 참조하고 변동 사항을 반영한다.

## 문서 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-28 | System | 문서 정리 및 구조화 |
| | | | - 중복 문서 통합 (계좌 API, 비용 최적화) |
| | | | - 일시적 설치 가이드 별도 폴더로 분리 |
| | | | - 모든 문서에 버전 히스토리 섹션 추가 |
| | | | - 문서 상태 표시 추가 |
| 1.1 | 2026-01-28 | System | 한국투자증권 MCP 통합 |
| | | | - MCP 통합 가이드 추가 |
| | | | - 한국투자증권 API 가이드에 MCP 사용 섹션 추가 |
| | | | - 시장 데이터 API 엔드포인트 문서 추가 |
| 1.2 | 2026-01-29 | System | 기획 문서 구조화 |
| | | | - PRD.md 생성 (제품 요구사항 통합 문서) |
| | | | - roadmap.md 생성 (프로젝트 로드맵) |
| | | | - decisions.md 생성 (아키텍처 결정 사항) |
| | | | - MCP 기획 문서 관리 가이드 추가 |
| 1.3 | 2026-01-29 | System | 자동투자 기획 반영 |
| | | | - 09-planning(화면·메뉴 기획서), 12·13 아키텍처(자동투자 전략·뉴스 수집) 문서 링크 추가 |
| | | | - decisions에 시장(KR/US)·뉴스 파이프라인 ADR 추가 |

## 참고 자료

- [프로젝트 README](../README.md)
- [PRD](./PRD.md) - 제품 요구사항 문서
- [로드맵](./roadmap.md) - 프로젝트 개발 계획
- [아키텍처 결정 사항](./decisions.md) - 기술 결정 기록
- [한국투자증권 API 가이드](./04-api/09-korea-investment-api-guide.md)
- [MCP 기획 문서 관리 가이드](./08-setup-guides/07-mcp-planning-guide.md)
