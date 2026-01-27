# Investment Choi 로컬 환경 확인 스크립트
# 사용법: .\scripts\check-local-env.ps1

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Investment Choi 로컬 환경 확인" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$allOk = $true

# 1. Java 확인
Write-Host "[1/6] Java 확인 중..." -ForegroundColor Yellow
try {
    $javaOutput = java -version 2>&1 | Out-String
    if ($javaOutput -match "version\s+""?(\d+)") {
        $javaVersion = $matches[1]
        if ([int]$javaVersion -ge 17) {
            Write-Host "  ✅ Java $javaVersion 설치됨" -ForegroundColor Green
        } else {
            Write-Host "  ❌ Java 17 이상이 필요합니다. 현재: Java $javaVersion" -ForegroundColor Red
            $allOk = $false
        }
    } else {
        Write-Host "  ❌ Java가 설치되지 않았습니다." -ForegroundColor Red
        Write-Host "     다운로드: https://adoptium.net/" -ForegroundColor Yellow
        $allOk = $false
    }
} catch {
    Write-Host "  ❌ Java 확인 실패: $_" -ForegroundColor Red
    $allOk = $false
}

# 2. Gradle 확인
Write-Host "`n[2/6] Gradle 확인 중..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    try {
        $gradleOutput = .\gradlew.bat -v 2>&1 | Out-String
        if ($gradleOutput -match "Gradle\s+(\d+\.\d+)") {
            $gradleVersion = $matches[1]
            Write-Host "  ✅ Gradle Wrapper $gradleVersion 사용 가능" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️  Gradle Wrapper 버전 확인 실패" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  ⚠️  Gradle Wrapper 실행 실패: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  gradlew.bat을 찾을 수 없습니다." -ForegroundColor Yellow
}

# 3. MariaDB 확인
Write-Host "`n[3/6] MariaDB 확인 중..." -ForegroundColor Yellow
$mariadbOk = $false
try {
    # Windows 서비스 확인
    $mariadbService = Get-Service -Name "MariaDB*" -ErrorAction SilentlyContinue
    if ($mariadbService) {
        if ($mariadbService.Status -eq "Running") {
            Write-Host "  ✅ MariaDB 서비스 실행 중: $($mariadbService.Name)" -ForegroundColor Green
            $mariadbOk = $true
        } else {
            Write-Host "  ⚠️  MariaDB 서비스 중지됨: $($mariadbService.Name)" -ForegroundColor Yellow
            Write-Host "     시작: Start-Service -Name '$($mariadbService.Name)'" -ForegroundColor Cyan
        }
    }
    
    # 연결 테스트
    if ($mariadbOk) {
        try {
            $testConnection = mysql -u investment -ppassword -h localhost -e "SELECT 1;" investment 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  ✅ MariaDB 연결 성공" -ForegroundColor Green
            } else {
                Write-Host "  ⚠️  MariaDB 연결 실패 (사용자/비밀번호 확인 필요)" -ForegroundColor Yellow
            }
        } catch {
            Write-Host "  ⚠️  mysql 클라이언트를 찾을 수 없습니다." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  ❌ MariaDB가 설치되지 않았습니다." -ForegroundColor Red
    Write-Host "     설치 가이드: docs\06-deployment\05-local-windows-setup.md" -ForegroundColor Yellow
    $allOk = $false
}

# 4. Redis 확인
Write-Host "`n[4/6] Redis 확인 중..." -ForegroundColor Yellow
$redisOk = $false
try {
    # WSL2 Redis 확인
    $wslRedis = wsl -e bash -c "redis-cli ping 2>&1" 2>&1
    if ($wslRedis -eq "PONG") {
        Write-Host "  ✅ Redis 실행 중 (WSL2)" -ForegroundColor Green
        $redisOk = $true
    }
} catch {
    # Memurai 확인
    try {
        $memuraiService = Get-Service -Name "Memurai*" -ErrorAction SilentlyContinue
        if ($memuraiService) {
            if ($memuraiService.Status -eq "Running") {
                Write-Host "  ✅ Memurai 실행 중" -ForegroundColor Green
                $redisOk = $true
            } else {
                Write-Host "  ⚠️  Memurai 서비스 중지됨" -ForegroundColor Yellow
                Write-Host "     시작: Start-Service -Name '$($memuraiService.Name)'" -ForegroundColor Cyan
            }
        }
    } catch {
        # Docker Redis 확인
        try {
            $dockerRedis = docker ps --filter "name=redis" --format "{{.Names}}" 2>&1
            if ($dockerRedis) {
                Write-Host "  ✅ Redis 실행 중 (Docker): $dockerRedis" -ForegroundColor Green
                $redisOk = $true
            }
        } catch {
            # redis-cli 직접 확인
            try {
                $redisCli = redis-cli ping 2>&1
                if ($redisCli -eq "PONG") {
                    Write-Host "  ✅ Redis 실행 중" -ForegroundColor Green
                    $redisOk = $true
                }
            } catch { }
        }
    }
}

if (-not $redisOk) {
    Write-Host "  ❌ Redis가 실행되지 않았습니다." -ForegroundColor Red
    Write-Host "     설치 가이드: docs\06-deployment\05-local-windows-setup.md" -ForegroundColor Yellow
    $allOk = $false
}

# 5. Python 확인
Write-Host "`n[5/6] Python 확인 중..." -ForegroundColor Yellow
$pythonOk = $false
try {
    $pythonVersion = python --version 2>&1
    if ($pythonVersion -match "Python\s+3\.(1[1-9]|[2-9][0-9])") {
        Write-Host "  ✅ $pythonVersion 설치됨" -ForegroundColor Green
        $pythonOk = $true
        
        # 가상환경 확인
        $venvPath = "ai-service\prediction-service\venv"
        if (Test-Path $venvPath) {
            Write-Host "  ✅ Python 가상환경 존재" -ForegroundColor Green
            
            # 패키지 확인
            $venvPython = Join-Path $venvPath "Scripts\python.exe"
            if (Test-Path $venvPython) {
                $fastapiCheck = & $venvPython -c "import fastapi; print('OK')" 2>&1
                if ($fastapiCheck -eq "OK") {
                    Write-Host "  ✅ FastAPI 설치됨" -ForegroundColor Green
                } else {
                    Write-Host "  ⚠️  FastAPI 미설치. 실행: pip install -r requirements.txt" -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "  ⚠️  Python 가상환경이 없습니다." -ForegroundColor Yellow
            Write-Host "     생성: cd ai-service\prediction-service && python -m venv venv" -ForegroundColor Cyan
        }
    } else {
        Write-Host "  ❌ Python 3.11 이상이 필요합니다." -ForegroundColor Red
        Write-Host "     현재: $pythonVersion" -ForegroundColor Yellow
        Write-Host "     다운로드: https://www.python.org/downloads/" -ForegroundColor Yellow
        $allOk = $false
    }
} catch {
    Write-Host "  ❌ Python이 설치되지 않았습니다." -ForegroundColor Red
    Write-Host "     다운로드: https://www.python.org/downloads/" -ForegroundColor Yellow
    $allOk = $false
}

# 6. Docker 확인 (선택)
Write-Host "`n[6/6] Docker 확인 중 (선택사항)..." -ForegroundColor Yellow
try {
    $dockerVersion = docker --version 2>&1
    if ($dockerVersion) {
        Write-Host "  ✅ Docker 설치됨: $dockerVersion" -ForegroundColor Green
    }
} catch {
    Write-Host "  ⚠️  Docker가 설치되지 않았습니다. (선택사항)" -ForegroundColor Yellow
}

# 결과 요약
Write-Host "`n========================================" -ForegroundColor Cyan
if ($allOk) {
    Write-Host "  ✅ 모든 필수 항목이 준비되었습니다!" -ForegroundColor Green
    Write-Host "`n다음 단계:" -ForegroundColor Cyan
    Write-Host "  1. Spring Boot 실행: .\gradlew.bat bootRun" -ForegroundColor White
    Write-Host "  2. AI 서비스 실행: cd ai-service\prediction-service && .\venv\Scripts\Activate.ps1 && uvicorn app.main:app --reload" -ForegroundColor White
} else {
    Write-Host "  ⚠️  일부 항목이 준비되지 않았습니다." -ForegroundColor Yellow
    Write-Host "`n설치 가이드 참고:" -ForegroundColor Cyan
    Write-Host "  docs\06-deployment\05-local-windows-setup.md" -ForegroundColor White
}
Write-Host "========================================`n" -ForegroundColor Cyan
