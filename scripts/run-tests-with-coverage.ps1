# 테스트 + JaCoCo 커버리지 (Agent 전용: 임시 빌드 사용, 실행 후 삭제)
# 임시 빌드: agent-build-<timestamp> → Gradle이 .agent-build-dir에 경로 기록 → 리포트 복사 후 삭제
# 리포트 복사: coverage-report/ (프로젝트 루트, build 폴더는 IntelliJ용으로 건드리지 않음)
# 사용법: .\scripts\run-tests-with-coverage.ps1  또는  -NoUniqueDir (IntelliJ build 사용)
# 주의: 8084 포트 사용 중이면 반드시 종료 후 실행. Windows에서 삭제 오류 발생 시 -NoUniqueDir 로 커버리지 생성.

param(
    [switch]$NoUniqueDir
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$agentBuildDirFile = Join-Path $rootDir ".agent-build-dir"
$coverageReportDir = Join-Path $rootDir "coverage-report"

Push-Location $rootDir
try {
    if (-not $NoUniqueDir) {
        Get-ChildItem -Path $rootDir -Directory -Filter "agent-build-*" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        if (Test-Path "$rootDir\agent-build") {
            Remove-Item "$rootDir\agent-build" -Recurse -Force -ErrorAction SilentlyContinue
            if (Test-Path "$rootDir\agent-build") { Write-Host "참고: agent-build 삭제 실패(파일 잠금). IntelliJ 등 다른 프로세스를 닫은 뒤 재실행하거나 -NoUniqueDir 사용." -ForegroundColor Yellow }
        }
        Remove-Item $agentBuildDirFile -Force -ErrorAction SilentlyContinue
        $env:GRADLE_UNIQUE_BUILD_DIR = "1"
        Write-Host "테스트 + 커버리지 (임시 빌드: agent-build-<timestamp>, 실행 후 삭제)" -ForegroundColor Cyan
    } else {
        Remove-Item Env:\GRADLE_UNIQUE_BUILD_DIR -ErrorAction SilentlyContinue
        Write-Host "테스트 + 커버리지 (빌드: 프로젝트 build)" -ForegroundColor Cyan
    }
    & .\gradlew test jacocoTestReport --no-daemon @args
    $code = $LASTEXITCODE
    if (-not $NoUniqueDir -and (Test-Path $agentBuildDirFile)) {
        $agentBuild = (Get-Content $agentBuildDirFile -Raw).Trim()
        if ($code -eq 0 -and $agentBuild -and (Test-Path $agentBuild)) {
            $jacocoHtml = Join-Path $agentBuild "reports\jacoco\test\html"
            if (Test-Path $jacocoHtml) {
                if (Test-Path $coverageReportDir) { Remove-Item $coverageReportDir -Recurse -Force }
                New-Item -ItemType Directory -Path $coverageReportDir -Force | Out-Null
                Copy-Item (Join-Path $jacocoHtml "*") $coverageReportDir -Recurse -Force
                Write-Host "커버리지 HTML: $coverageReportDir\index.html" -ForegroundColor Green
            }
        }
        if ($agentBuild -and (Test-Path $agentBuild)) {
            Remove-Item $agentBuild -Recurse -Force -ErrorAction SilentlyContinue
        }
        Remove-Item $agentBuildDirFile -Force -ErrorAction SilentlyContinue
    }
    exit $code
} finally {
    Pop-Location
    Remove-Item Env:\GRADLE_UNIQUE_BUILD_DIR -ErrorAction SilentlyContinue
}
