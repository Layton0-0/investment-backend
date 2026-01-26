# AI 기반 자동투자 시스템 구현 계획

## 구현 우선순위

### Phase 1: 기반 인프라 구축 (완료 중)
- [x] Spring Boot 3.x 업그레이드
- [x] Java 17 마이그레이션
- [ ] Redis 캐싱 레이어 추가
- [ ] OpenAPI 문서화
- [ ] Circuit Breaker 패턴 적용

### Phase 2: AI/ML 서비스 통합
- [ ] AI 서비스 클라이언트 인터페이스 설계
- [ ] Python FastAPI 서비스 기본 구조
- [ ] LSTM 예측 모델 개발
- [ ] 모델 서빙 API 구현

### Phase 3: 실시간 데이터 처리
- [ ] WebSocket 지원
- [ ] 실시간 시장 데이터 스트리밍
- [ ] 이벤트 기반 아키텍처 (Kafka 준비)

### Phase 4: 고급 AI 기능
- [ ] 강화학습 에이전트
- [ ] NLP 서비스
- [ ] Vector DB 통합

## 현재 진행 상황

### 완료된 작업
1. 재설계 문서 작성 (`docs/02-architecture/03-ai-redesign.md`)
2. Spring Boot 3.x 업그레이드 시작

### 진행 중인 작업
1. build.gradle 업그레이드
2. AI 서비스 통합 인터페이스 설계
