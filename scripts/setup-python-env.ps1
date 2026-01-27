# Python 가상환경 설정 스크립트
# 사용법: .\scripts\setup-python-env.ps1

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Python 가상환경 설정" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Python 확인
Write-Host "[1/4] Python 확인 중..." -ForegroundColor Yellow
try {
    $pythonVersion = python --version 2>&1
    if ($pythonVersion -match "Python\s+3\.(1[1-9]|[2-9][0-9])") {
        Write-Host "  ✅ $pythonVersion" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Python 3.11 이상이 필요합니다." -ForegroundColor Red
        Write-Host "     현재: $pythonVersion" -ForegroundColor Yellow
        exit 1
    }
} catch {
    Write-Host "  ❌ Python이 설치되지 않았습니다." -ForegroundColor Red
    exit 1
}

# 디렉토리 이동
$aiServicePath = "ai-service\prediction-service"
if (-not (Test-Path $aiServicePath)) {
    Write-Host "  ❌ $aiServicePath 디렉토리를 찾을 수 없습니다." -ForegroundColor Red
    exit 1
}

Set-Location $aiServicePath
Write-Host "`n[2/4] 작업 디렉토리: $(Get-Location)" -ForegroundColor Yellow

# 가상환경 생성
Write-Host "`n[3/4] 가상환경 생성 중..." -ForegroundColor Yellow
if (Test-Path "venv") {
    Write-Host "  ⚠️  가상환경이 이미 존재합니다. 삭제 후 재생성하시겠습니까? (Y/N)" -ForegroundColor Yellow
    $response = Read-Host
    if ($response -eq "Y" -or $response -eq "y") {
        Remove-Item -Recurse -Force "venv"
        Write-Host "  ✅ 기존 가상환경 삭제됨" -ForegroundColor Green
    } else {
        Write-Host "  ℹ️  기존 가상환경 사용" -ForegroundColor Cyan
        Set-Location ..\..
        exit 0
    }
}

python -m venv venv
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ❌ 가상환경 생성 실패" -ForegroundColor Red
    Set-Location ..\..
    exit 1
}
Write-Host "  ✅ 가상환경 생성 완료" -ForegroundColor Green

# 가상환경 활성화
Write-Host "`n[4/4] 패키지 설치 중..." -ForegroundColor Yellow
& .\venv\Scripts\Activate.ps1

# pip 업그레이드
Write-Host "  pip 업그레이드 중..." -ForegroundColor Cyan
python -m pip install --upgrade pip

# requirements.txt 확인
if (Test-Path "requirements.txt") {
    Write-Host "  requirements.txt에서 패키지 설치 중..." -ForegroundColor Cyan
    pip install -r requirements.txt
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✅ 패키지 설치 완료" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  일부 패키지 설치 실패" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ⚠️  requirements.txt를 찾을 수 없습니다." -ForegroundColor Yellow
}

# PyTorch CPU 버전 확인 및 설치
Write-Host "`n  PyTorch CPU 버전 확인 중..." -ForegroundColor Cyan
$torchCheck = python -c "import torch; print(torch.__version__)" 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "  ✅ PyTorch 설치됨: $torchCheck" -ForegroundColor Green
} else {
    Write-Host "  PyTorch CPU 버전 설치 중..." -ForegroundColor Cyan
    pip install torch --index-url https://download.pytorch.org/whl/cpu
}

# 설치 확인
Write-Host "`n설치된 주요 패키지:" -ForegroundColor Cyan
python -c "import fastapi; print(f'  FastAPI: {fastapi.__version__}')" 2>&1
python -c "import uvicorn; print(f'  Uvicorn: {uvicorn.__version__}')" 2>&1
python -c "import torch; print(f'  PyTorch: {torch.__version__}')" 2>&1
python -c "import numpy; print(f'  NumPy: {numpy.__version__}')" 2>&1
python -c "import pandas; print(f'  Pandas: {pandas.__version__}')" 2>&1

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  ✅ Python 가상환경 설정 완료!" -ForegroundColor Green
Write-Host "`n가상환경 활성화:" -ForegroundColor Cyan
Write-Host "  .\venv\Scripts\Activate.ps1" -ForegroundColor White
Write-Host "`nFastAPI 서버 실행:" -ForegroundColor Cyan
Write-Host "  uvicorn app.main:app --reload --host 0.0.0.0 --port 8000" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan

Set-Location ..\..
