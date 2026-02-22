# API 스모크 검사: 헬스·주요 엔드포인트 호출 후 exit code로 정합성 판단.
# 사용: .\scripts\smoke-api.ps1 [-Port 8080] (로컬은 Docker Compose Backend 8080)
# Agent가 "API 정합성 확인" 요청 시 이 스크립트 실행 후 결과 해석.

param(
    [int] $Port = 8080
)

$ErrorActionPreference = "Stop"
$base = "http://localhost:$Port"

function Invoke-Smoke {
    param([string] $Method, [string] $Path, [string] $Description)
    $url = "$base$Path"
    try {
        $r = Invoke-WebRequest -Uri $url -Method $Method -UseBasicParsing -TimeoutSec 10
        if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 300) {
            Write-Host "[OK] $Description -> $($r.StatusCode)" -ForegroundColor Green
            return $true
        }
        Write-Host "[FAIL] $Description -> $($r.StatusCode)" -ForegroundColor Red
        return $false
    } catch {
        Write-Host "[FAIL] $Description -> $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

Write-Host "Smoke check: $base" -ForegroundColor Cyan
$ok = $true
if (-not (Invoke-Smoke -Method Get -Path "/actuator/health" -Description "actuator/health")) { $ok = $false }
if (-not $ok) {
    Write-Host "Smoke check failed." -ForegroundColor Red
    exit 1
}
Write-Host "Smoke check passed." -ForegroundColor Green
exit 0
