# 테스트 실행 가이드

## 표준 명령 (권장)

**Windows (PowerShell / CMD)**

```powershell
.\gradlew.bat test --no-daemon
```

- `--no-daemon`: Gradle 데몬을 사용하지 않고 실행. 로컬에서 테스트만 돌릴 때 안정적으로 동작하며, VM 경고(Sharing is only supported for boot loader classes...)가 나와도 테스트는 정상 실행된다.
- 이 방식으로 실행하면 앞으로도 동일하게 테스트를 실행하면 된다.

**Linux / macOS**

```bash
./gradlew test --no-daemon
```

## 스크립트 사용 (선택)

- **테스트만 실행** (임시 빌드 폴더 사용 후 삭제, Agent/CI 용도):  
  `.\scripts\run-tests.ps1`  
  → 내부적으로 `.\gradlew test --no-daemon` 호출.

- **테스트 + 커버리지**:  
  `.\scripts\run-tests-with-coverage.ps1`  
  → 내부적으로 `.\gradlew test jacocoTestReport --no-daemon` 호출.

- IntelliJ와 빌드 폴더를 함께 쓰는 경우:  
  `.\scripts\run-tests.ps1 -NoUniqueDir` 또는  
  `.\scripts\run-tests-with-coverage.ps1 -NoUniqueDir`

## 특정 테스트만 실행

```powershell
# 특정 클래스
.\gradlew.bat test --no-daemon --tests "com.investment.factor.service.PositionSizingServiceTest"

# 특정 메서드
.\gradlew.bat test --no-daemon --tests "com.investment.factor.service.PositionSizingServiceTest.getRecommendations_withHalfKelly_adjustsPositionSize"
```

## 참고

- 빌드/테스트 리포트: `build/reports/tests/test/index.html`
- JaCoCo 커버리지: `.\gradlew.bat test jacocoTestReport --no-daemon` 후 `build/reports/jacoco/test/html/index.html`
- 커버리지 한계값 검증: `.\gradlew.bat jacocoTestCoverageVerification --no-daemon`
