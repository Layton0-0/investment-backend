# 대시보드·UX 개선 현황

> **문서 상태**: 1.0 (2026-02-20)  
> **분류**: 기능 현황  
> **관련 문서**: `01-screen-menu-spec.md`, `10-design-ai-full-prompt.md`

---

## 1. 구현 현황 요약

### 1.1 완료된 항목

| 항목 | 설명 | 파일 |
|------|------|------|
| **토스트 알림 시스템** | 성공/에러/경고/정보 알림 컴포넌트 | `common.css`, `toast.js` |
| **반응형 모바일 레이아웃** | 768px 이하 햄버거 메뉴, 1열 레이아웃 | `common.css`, `mobile-nav.js` |
| **차트 플레이스홀더** | 차트 영역 스타일, 로딩 애니메이션 | `common.css` |
| **스켈레톤 로딩** | 데이터 로딩 중 UI 상태 | `common.css` |
| **실시간 데이터 표시** | 연결 상태, 가격 변동 표시 | `common.css` |

### 1.2 진행 예정

| 항목 | 우선순위 | 설명 |
|------|----------|------|
| 실시간 차트 확장 | 중 | Chart.js/ApexCharts 통합 |
| 앱 내 알림 연동 | 중 | WebSocket 기반 푸시 알림 |
| PWA 지원 | 낮 | Service Worker, 오프라인 캐시 |

---

## 2. 토스트 알림 사용법

### 2.1 기본 사용

```html
<script src="/js/toast.js"></script>
<script>
// 성공 알림
Toast.success('저장 완료', '설정이 저장되었습니다.');

// 에러 알림
Toast.error('오류', '요청 처리에 실패했습니다.');

// 경고 알림
Toast.warning('주의', '일일 손실 한도에 근접했습니다.');

// 정보 알림
Toast.info('안내', '자동투자가 시작되었습니다.');
</script>
```

### 2.2 고급 옵션

```javascript
Toast.show({
    type: 'success',      // 'success' | 'error' | 'warning' | 'info'
    title: '제목',
    message: '상세 메시지',
    duration: 5000        // ms, 0이면 자동 닫기 안 함
});

// 수동 닫기
var toast = Toast.success('제목', '메시지');
toast.dismiss();
```

---

## 3. 반응형 브레이크포인트

| 구분 | 너비 | 레이아웃 |
|------|------|----------|
| 데스크톱 | 1025px+ | 1200px 컨테이너, 사이드 메뉴 |
| 태블릿 | 769~1024px | 1200px 컨테이너, 축소 그리드 |
| 모바일 | ~768px | 1열, 햄버거 메뉴 |
| 소형 모바일 | ~480px | 1열, 최소 패딩 |

---

## 4. 차트 통합 가이드

### 4.1 Chart.js 권장 구성

```html
<div class="chart-container">
    <canvas id="priceChart" class="chart-canvas"></canvas>
</div>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script>
new Chart(document.getElementById('priceChart'), {
    type: 'line',
    data: { /* ... */ },
    options: {
        responsive: true,
        maintainAspectRatio: false
    }
});
</script>
```

### 4.2 로딩 상태

```html
<div class="chart-container">
    <div class="chart-loading"></div>
</div>
```

### 4.3 빈 상태

```html
<div class="chart-container">
    <div class="chart-placeholder">
        <div class="icon">📈</div>
        <div class="text">데이터가 없습니다</div>
    </div>
</div>
```

---

## 5. 스켈레톤 로딩 사용

```html
<!-- 텍스트 로딩 -->
<div class="skeleton skeleton-text"></div>
<div class="skeleton skeleton-text short"></div>

<!-- 박스 로딩 -->
<div class="skeleton skeleton-box"></div>
```

---

## 6. 실시간 데이터 표시

### 6.1 연결 상태

```html
<span class="realtime-indicator">
    <span class="dot"></span>
    <span>실시간</span>
</span>

<!-- 연결 끊김 -->
<span class="realtime-indicator disconnected">
    <span class="dot"></span>
    <span>연결 끊김</span>
</span>
```

### 6.2 가격 변동

```html
<span class="price-change up">
    <span class="arrow">▲</span>
    <span>+2.35%</span>
</span>

<span class="price-change down">
    <span class="arrow">▼</span>
    <span>-1.12%</span>
</span>
```

---

## 7. 후속 개발 계획

### Phase 1: 차트 확장 (다음 스프린트)
- [ ] 수익 곡선 차트 (백테스트 결과)
- [ ] 포트폴리오 섹터 파이 차트
- [ ] 일봉 캔들스틱 차트

### Phase 2: 알림 고도화
- [ ] WebSocket 실시간 알림
- [ ] 알림 센터 (읽음/안읽음)
- [ ] 브라우저 푸시 알림

### Phase 3: PWA
- [ ] manifest.json
- [ ] Service Worker
- [ ] 오프라인 캐시

---

## 변경 이력

| 버전 | 일자 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-02-20 | System | 초기 문서 작성 - 토스트, 반응형, 차트 스타일 구현 |
