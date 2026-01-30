# Gradle Wrapper JAR 설치 스크립트 (winget 없이)
# 프로젝트 루트에서 실행: .\scripts\setup-gradle-wrapper.ps1
# 또는: powershell -ExecutionPolicy Bypass -File .\scripts\setup-gradle-wrapper.ps1

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$WrapperDir = Join-Path $ProjectRoot "gradle\wrapper"
$JarPath = Join-Path $WrapperDir "gradle-wrapper.jar"

$Urls = @(
    "https://github.com/gradle/gradle/raw/v7.6.3/gradle/wrapper/gradle-wrapper.jar",
    "https://services.gradle.org/distributions/gradle-7.6.3-bin.zip"
)

if (Test-Path $JarPath) {
    Write-Host "gradle-wrapper.jar already exists. Skipping." -ForegroundColor Green
    exit 0
}

[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

# 1) wrapper jar 직접 다운로드 시도
try {
    Write-Host "Downloading gradle-wrapper.jar from GitHub..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $Urls[0] -OutFile $JarPath -UseBasicParsing
    Write-Host "Done. Run: .\gradlew.bat -v" -ForegroundColor Green
    exit 0
} catch {
    Write-Host "Direct jar download failed: $_" -ForegroundColor Yellow
}

# 2) Gradle 배포판 zip에서 jar 추출
$ZipPath = Join-Path $env:TEMP "gradle-7.6.3-bin.zip"
try {
    Write-Host "Downloading Gradle 7.6.3 distribution..." -ForegroundColor Cyan
    Invoke-WebRequest -Uri $Urls[1] -OutFile $ZipPath -UseBasicParsing
    $ExtractDir = Join-Path $env:TEMP "gradle-7.6.3-extract"
    Expand-Archive -Path $ZipPath -DestinationPath $ExtractDir -Force
    $JarInZip = Join-Path $ExtractDir "gradle-7.6.3\gradle\wrapper\gradle-wrapper.jar"
    if (Test-Path $JarInZip) {
        Copy-Item $JarInZip -Destination $JarPath -Force
        Write-Host "Done. Run: .\gradlew.bat -v" -ForegroundColor Green
    } else {
        throw "gradle-wrapper.jar not found in zip"
    }
    Remove-Item $ZipPath -Force -ErrorAction SilentlyContinue
    Remove-Item $ExtractDir -Recurse -Force -ErrorAction SilentlyContinue
} catch {
    Write-Host "Fallback download failed: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "Manual step: Download gradle-wrapper.jar and save to:" -ForegroundColor Yellow
    Write-Host "  $JarPath" -ForegroundColor White
    Write-Host "  URL: $($Urls[0])" -ForegroundColor Gray
    exit 1
}
