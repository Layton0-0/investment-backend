# Cursor/Agent 전용 서버 실행 (포트 8084)
# 사용: .\scripts\bootRun-agent.ps1
# 기존 local(8083)과 동시에 별도 서버 실행 가능
# build 잠금 회피: GRADLE_UNIQUE_BUILD_DIR 설정 시 임시 디렉터리에 빌드 (run-tests.ps1과 동일)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

$env:GRADLE_UNIQUE_BUILD_DIR = "1"
Write-Host "Starting investment-choi on port 8084 (profile: local,local-agent, build: 임시 디렉터리)..." -ForegroundColor Cyan
.\gradlew.bat bootRun --args="--spring.profiles.active=local,local-agent"
