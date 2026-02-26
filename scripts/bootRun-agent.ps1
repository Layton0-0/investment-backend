# Cursor/Agent 전용 서버 실행 (포트 8084)
# 사용: .\scripts\bootRun-agent.ps1
# 기존 local(8083)과 동시에 별도 서버 실행 가능
# .env가 있으면 로드하여 SUPER_ADMIN_* 등 환경변수 전달 (E2E·로그인 검증 시 동일 계정 사용).
# Agent 실행 시 터미널 타임아웃 300000ms(5분) 이상 권장 — .cursor/rules/script-run-timeouts.mdc

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

$envFile = Join-Path $PSScriptRoot "..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$' -and $line -notmatch '^\s*#') {
            $key = $Matches[1]
            $val = $Matches[2].Trim().Trim('"').TrimEnd([char]13)
            [Environment]::SetEnvironmentVariable($key, $val, "Process")
        }
    }
    Write-Host "Loaded env from .env" -ForegroundColor Gray
}

$env:GRADLE_UNIQUE_BUILD_DIR = "1"
Write-Host "Starting investment-choi on port 8084 (profile: local,local-agent, build: 임시 디렉터리)..." -ForegroundColor Cyan
.\gradlew.bat bootRun --args="--spring.profiles.active=local,local-agent --server.port=8084"
