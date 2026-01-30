# 테스트 실행 (Agent 전용: 임시 빌드 폴더 사용, build 폴더는 IntelliJ용)
# 임시 빌드: agent-build-<timestamp> → Gradle이 .agent-build-dir에 경로 기록 → 실행 후 삭제
# 사용법: .\scripts\run-tests.ps1  또는  .\scripts\run-tests.ps1 -NoUniqueDir (IntelliJ build 사용)
# 주의: 8084 포트 사용 중이면 반드시 종료 후 실행. Windows에서 "Unable to delete directory test-results\test\binary" 발생 시 -NoUniqueDir 사용.

param(
    [switch]$NoUniqueDir
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
$agentBuildDirFile = Join-Path $rootDir ".agent-build-dir"

Push-Location $rootDir
try {
    if (-not $NoUniqueDir) {
        Get-ChildItem -Path $rootDir -Directory -Filter "agent-build-*" -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item $agentBuildDirFile -Force -ErrorAction SilentlyContinue
        $env:GRADLE_UNIQUE_BUILD_DIR = "1"
        Write-Host "테스트 실행 (임시 빌드: agent-build-<timestamp>, 실행 후 삭제)" -ForegroundColor Cyan
    } else {
        Remove-Item Env:\GRADLE_UNIQUE_BUILD_DIR -ErrorAction SilentlyContinue
        Write-Host "테스트 실행 (빌드: 프로젝트 build)" -ForegroundColor Cyan
    }
    & .\gradlew test --no-daemon @args
    $code = $LASTEXITCODE
    if (-not $NoUniqueDir -and (Test-Path $agentBuildDirFile)) {
        $agentBuild = Get-Content $agentBuildDirFile -Raw
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
