# 테스트 실행 스크립트 (build 디렉터리 잠금 회피)
# Windows 등에서 build 디렉터리 삭제 실패 시, 시스템 임시 디렉터리에 빌드하여 실행합니다.
# 사용법: .\scripts\run-tests.ps1  또는  .\scripts\run-tests.ps1 --no-unique-dir (기본 build 사용)

param(
    [switch]$NoUniqueDir  # 이 옵션을 주면 기존 build 폴더 사용 (잠금 없을 때만)
)

$ErrorActionPreference = "Stop"
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent $scriptDir
Push-Location $rootDir
try {
    if (-not $NoUniqueDir) {
        $env:GRADLE_UNIQUE_BUILD_DIR = "1"
        Write-Host "테스트 실행 (빌드 경로: 시스템 임시 디렉터리, build 잠금 회피)" -ForegroundColor Cyan
    } else {
        Remove-Item Env:\GRADLE_UNIQUE_BUILD_DIR -ErrorAction SilentlyContinue
        Write-Host "테스트 실행 (빌드 경로: 프로젝트 build)" -ForegroundColor Cyan
    }
    & .\gradlew test --no-daemon @args
    exit $LASTEXITCODE
} finally {
    Pop-Location
    Remove-Item Env:\GRADLE_UNIQUE_BUILD_DIR -ErrorAction SilentlyContinue
}
