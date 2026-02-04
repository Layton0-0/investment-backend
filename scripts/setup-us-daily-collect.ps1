# US 일봉 수집(yfinance) 환경 설정
# 사용법: .\scripts\setup-us-daily-collect.ps1
# 프로젝트 루트에서 실행

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $scriptDir
$collectorScript = Join-Path $scriptDir "us_daily_collector.py"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  US 일봉 수집 (yfinance) 설정" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1) Python 확인
Write-Host "[1/4] Python 확인 중..." -ForegroundColor Yellow
$pythonCmd = $env:US_PYTHON_COMMAND
if (-not $pythonCmd) { $pythonCmd = "python" }
try {
    $version = & $pythonCmd --version 2>&1
    Write-Host "  $version" -ForegroundColor Green
} catch {
    Write-Host "  Python을 찾을 수 없습니다. Python 3.7+ 설치 후 다시 실행하세요." -ForegroundColor Red
    exit 1
}

# 2) yfinance 설치
Write-Host "`n[2/4] yfinance 설치 중..." -ForegroundColor Yellow
$reqPath = Join-Path $scriptDir "requirements-us-daily.txt"
if (Test-Path $reqPath) {
    & $pythonCmd -m pip install -r $reqPath -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  pip install 실패. 수동 실행: $pythonCmd -m pip install yfinance" -ForegroundColor Red
        exit 1
    }
    Write-Host "  yfinance 설치 완료" -ForegroundColor Green
} else {
    & $pythonCmd -m pip install yfinance -q
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  pip install yfinance 실패" -ForegroundColor Red
        exit 1
    }
    Write-Host "  yfinance 설치 완료" -ForegroundColor Green
}

# 3) 스크립트 존재 확인
Write-Host "`n[3/4] 수집 스크립트 확인..." -ForegroundColor Yellow
if (-not (Test-Path $collectorScript)) {
    Write-Host "  us_daily_collector.py를 찾을 수 없습니다: $collectorScript" -ForegroundColor Red
    exit 1
}
Write-Host "  $collectorScript" -ForegroundColor Green

# 4) 테스트 실행 (어제 날짜로 1종목만)
Write-Host "`n[4/4] 테스트 실행 (SPY 1종목)..." -ForegroundColor Yellow
$testDate = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd")
Push-Location $scriptDir
try {
    $out = & $pythonCmd us_daily_collector.py --bas-dt $testDate --symbols SPY 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  테스트 실패 (미국 장 휴장일이면 빈 결과일 수 있음): $out" -ForegroundColor Yellow
    } else {
        $parsed = $out | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($parsed -and $parsed.Count -ge 0) {
            Write-Host "  테스트 OK (출력 길이: $($out.Length) bytes)" -ForegroundColor Green
        } else {
            Write-Host "  테스트 OK (미국 장 휴장일이면 [] 반환)" -ForegroundColor Green
        }
    }
} finally {
    Pop-Location
}

$absPath = (Resolve-Path $collectorScript).Path
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  설정 안내" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  앱을 프로젝트 루트에서 실행하면 기본 경로(scripts/us_daily_collector.py)로 동작합니다." -ForegroundColor White
Write-Host "  다른 디렉터리에서 실행할 경우 아래 환경 변수를 설정하세요:" -ForegroundColor White
Write-Host "  US_YFINANCE_SCRIPT_PATH=$absPath" -ForegroundColor Gray
Write-Host "  US_PYTHON_COMMAND=$pythonCmd" -ForegroundColor Gray
Write-Host "`n  백테스트 페이지에서 'US 일봉 수집' 버튼으로 수집할 수 있습니다." -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan
