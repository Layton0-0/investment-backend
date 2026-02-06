# US 일봉 수집 설정 안내 (Polyrepo: investment-data-collector 사용)
# 사용법: .\scripts\setup-us-daily-collect.ps1
# 프로젝트 루트(investment-backend)에서 실행

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  US 일봉 수집 설정 (investment-data-collector)" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

Write-Host "US 일봉 수집은 별도 레포 'investment-data-collector' 서비스를 사용합니다." -ForegroundColor White
Write-Host "  - 레포: services/data-collector (또는 investment-data-collector clone)" -ForegroundColor Gray
Write-Host "  - 서비스: FastAPI, POST /us-daily, GET /health, 기본 포트 8001`n" -ForegroundColor Gray

Write-Host "[1] data-collector 서비스 기동" -ForegroundColor Yellow
Write-Host "  로컬: cd services\data-collector; pip install -r requirements.txt; uvicorn app:app --host 0.0.0.0 --port 8001" -ForegroundColor Gray
Write-Host "  또는 Docker: infra/docker-compose.local.yml 에서 해당 서비스 추가 후 up`n" -ForegroundColor Gray

Write-Host "[2] 백엔드 설정" -ForegroundColor Yellow
Write-Host "  application.yml 또는 환경 변수:" -ForegroundColor Gray
Write-Host "  investment.data.us.collector-url: http://localhost:8001" -ForegroundColor Gray
Write-Host "  또는 US_COLLECTOR_URL=http://localhost:8001`n" -ForegroundColor Gray

Write-Host "  yfinance-script-path 기본값은 비어 있습니다. collector-url을 사용하세요." -ForegroundColor Gray
Write-Host "========================================`n" -ForegroundColor Cyan
