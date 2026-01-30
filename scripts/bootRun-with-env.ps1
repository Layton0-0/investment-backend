# .env 파일을 로드한 뒤 bootRun 실행
# 사용: .\scripts\bootRun-with-env.ps1
# .env 변수는 application-local.yml / application.yml 에서 ${VAR:default} 형태로 참조됨.

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot\..

$envFile = Join-Path (Get-Location) ".env"
if (Test-Path $envFile) {
    Write-Host "Loading .env ..." -ForegroundColor Cyan
    Get-Content $envFile -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $idx = $line.IndexOf("=")
            if ($idx -gt 0) {
                $name = $line.Substring(0, $idx).Trim()
                $value = $line.Substring($idx + 1).Trim()
                if ($value -match '^["''](.*)["'']$') { $value = $matches[1] }
                [Environment]::SetEnvironmentVariable($name, $value, "Process")
            }
        }
    }
} else {
    Write-Host ".env not found. Create .env (see application-local.yml for variable names)." -ForegroundColor Yellow
}

.\gradlew.bat bootRun
