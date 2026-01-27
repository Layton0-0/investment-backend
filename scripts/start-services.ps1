# 서비스 시작 스크립트
# 사용법: .\scripts\start-services.ps1

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Investment Choi 서비스 시작" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# MariaDB 시작
Write-Host "[1/3] MariaDB 확인 중..." -ForegroundColor Yellow
try {
    $mariadbService = Get-Service -Name "MariaDB*" -ErrorAction SilentlyContinue
    if ($mariadbService) {
        if ($mariadbService.Status -ne "Running") {
            Start-Service -Name $mariadbService.Name
            Write-Host "  ✅ MariaDB 시작됨" -ForegroundColor Green
        } else {
            Write-Host "  ✅ MariaDB 이미 실행 중" -ForegroundColor Green
        }
    } else {
        Write-Host "  ⚠️  MariaDB 서비스를 찾을 수 없습니다." -ForegroundColor Yellow
    }
} catch {
    Write-Host "  ⚠️  MariaDB 시작 실패: $_" -ForegroundColor Yellow
}

# Redis 시작
Write-Host "`n[2/3] Redis 확인 중..." -ForegroundColor Yellow
$redisStarted = $false

# WSL2 Redis
try {
    $wslRedis = wsl -e bash -c "redis-cli ping 2>&1" 2>&1
    if ($wslRedis -eq "PONG") {
        Write-Host "  ✅ Redis 실행 중 (WSL2)" -ForegroundColor Green
        $redisStarted = $true
    } else {
        Write-Host "  WSL2에서 Redis 시작 중..." -ForegroundColor Cyan
        wsl -e bash -c "sudo service redis-server start" 2>&1 | Out-Null
        Start-Sleep -Seconds 2
        $wslRedis = wsl -e bash -c "redis-cli ping 2>&1" 2>&1
        if ($wslRedis -eq "PONG") {
            Write-Host "  ✅ Redis 시작됨 (WSL2)" -ForegroundColor Green
            $redisStarted = $true
        }
    }
} catch {
    # Memurai 확인
    try {
        $memuraiService = Get-Service -Name "Memurai*" -ErrorAction SilentlyContinue
        if ($memuraiService) {
            if ($memuraiService.Status -ne "Running") {
                Start-Service -Name $memuraiService.Name
                Write-Host "  ✅ Memurai 시작됨" -ForegroundColor Green
                $redisStarted = $true
            } else {
                Write-Host "  ✅ Memurai 이미 실행 중" -ForegroundColor Green
                $redisStarted = $true
            }
        }
    } catch { }
}

if (-not $redisStarted) {
    Write-Host "  ⚠️  Redis를 시작할 수 없습니다." -ForegroundColor Yellow
}

# Python 가상환경 확인
Write-Host "`n[3/3] Python 가상환경 확인 중..." -ForegroundColor Yellow
$venvPath = "ai-service\prediction-service\venv"
if (Test-Path $venvPath) {
    Write-Host "  ✅ Python 가상환경 존재" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Python 가상환경이 없습니다." -ForegroundColor Yellow
    Write-Host "     실행: .\scripts\setup-python-env.ps1" -ForegroundColor Cyan
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  ✅ 서비스 확인 완료" -ForegroundColor Green
Write-Host "`n다음 단계:" -ForegroundColor Cyan
Write-Host "  1. Spring Boot 실행 (새 터미널):" -ForegroundColor White
Write-Host "     .\gradlew.bat bootRun" -ForegroundColor Yellow
Write-Host "`n  2. AI 서비스 실행 (새 터미널):" -ForegroundColor White
Write-Host "     cd ai-service\prediction-service" -ForegroundColor Yellow
Write-Host "     .\venv\Scripts\Activate.ps1" -ForegroundColor Yellow
Write-Host "     uvicorn app.main:app --reload --host 0.0.0.0 --port 8000" -ForegroundColor Yellow
Write-Host "========================================`n" -ForegroundColor Cyan
