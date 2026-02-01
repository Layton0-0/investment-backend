# Investment Choi Prerequisites Check Script
# Usage: .\scripts\check-prerequisites.ps1
# Checks WSL2 + Docker Compose environment

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Investment Choi Prerequisites Check" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$allOk = $true
$warnings = @()

# 1. Windows Version Check
Write-Host "[1/8] Checking Windows version..." -ForegroundColor Yellow
try {
    $osVersion = [System.Environment]::OSVersion.Version
    $winVersion = (Get-CimInstance Win32_OperatingSystem).Version
    if ($winVersion -match "^10\.0\.(22000|22621|26100)") {
        Write-Host "  [OK] Windows 11 detected (version: $winVersion)" -ForegroundColor Green
    } elseif ($winVersion -match "^10\.0\.") {
        Write-Host "  [WARNING] Windows 10 detected. Windows 11 recommended" -ForegroundColor Yellow
        $warnings += "Windows 11 recommended"
    } else {
        Write-Host "  [WARNING] Windows version check needed" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [WARNING] Windows version check failed" -ForegroundColor Yellow
}

# 2. WSL2 Check
Write-Host "`n[2/8] Checking WSL2..." -ForegroundColor Yellow
$wslOk = $false
try {
    $wslVersion = wsl --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] WSL installed" -ForegroundColor Green
        
        # Check WSL2 distribution
        $wslList = wsl --list --verbose 2>&1
        if ($wslList -match "VERSION\s+2") {
            Write-Host "  [OK] WSL2 distribution found" -ForegroundColor Green
            $wslOk = $true
        } else {
            Write-Host "  [WARNING] WSL2 distribution not found" -ForegroundColor Yellow
            Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Cyan
            $warnings += "WSL2 distribution needed"
        }
    } else {
        Write-Host "  [ERROR] WSL not installed" -ForegroundColor Red
        Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
        Write-Host "     Or: wsl --install (admin required)" -ForegroundColor Cyan
        $allOk = $false
    }
} catch {
    Write-Host "  [ERROR] WSL check failed: $_" -ForegroundColor Red
    Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
    $allOk = $false
}

# 3. Docker Desktop Check
Write-Host "`n[3/8] Checking Docker Desktop..." -ForegroundColor Yellow
$dockerOk = $false
try {
    $dockerVersion = docker --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Docker installed: $dockerVersion" -ForegroundColor Green
        
        # Check if Docker is running
        $dockerPs = docker ps 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Docker running" -ForegroundColor Green
            $dockerOk = $true
        } else {
            Write-Host "  [WARNING] Docker Desktop is not running" -ForegroundColor Yellow
            Write-Host "     Please start Docker Desktop" -ForegroundColor Cyan
            $warnings += "Docker Desktop start needed"
        }
    } else {
        Write-Host "  [ERROR] Docker not installed" -ForegroundColor Red
        Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
        Write-Host "     Or download: https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
        $allOk = $false
    }
} catch {
    Write-Host "  [ERROR] Docker check failed: $_" -ForegroundColor Red
    Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
    $allOk = $false
}

# Docker Compose Check
if ($dockerOk) {
    try {
        $composeVersion = docker compose version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Docker Compose available: $composeVersion" -ForegroundColor Green
        } else {
            Write-Host "  [WARNING] Docker Compose check failed" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [WARNING] Docker Compose check failed" -ForegroundColor Yellow
    }
}

# 4. Java Check
Write-Host "`n[4/8] Checking Java..." -ForegroundColor Yellow
try {
    $javaOutput = java -version 2>&1 | Out-String
    if ($javaOutput -match "version\s+""?(\d+)") {
        $javaVersion = $matches[1]
        if ([int]$javaVersion -ge 17) {
            Write-Host "  [OK] Java $javaVersion installed" -ForegroundColor Green
        } else {
            Write-Host "  [ERROR] Java 17+ required. Current: Java $javaVersion" -ForegroundColor Red
            Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
            $allOk = $false
        }
    } else {
        Write-Host "  [ERROR] Java not installed" -ForegroundColor Red
        Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
        $allOk = $false
    }
} catch {
    Write-Host "  [ERROR] Java check failed: $_" -ForegroundColor Red
    Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
    $allOk = $false
}

# 5. Python Check
Write-Host "`n[5/8] Checking Python..." -ForegroundColor Yellow
$pythonOk = $false
try {
    $pythonVersion = python --version 2>&1
    if ($pythonVersion -match "Python\s+3\.(1[1-9]|[2-9][0-9])") {
        Write-Host "  [OK] $pythonVersion installed" -ForegroundColor Green
        $pythonOk = $true
    } else {
        Write-Host "  [ERROR] Python 3.11+ required" -ForegroundColor Red
        Write-Host "     Current: $pythonVersion" -ForegroundColor Yellow
        Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
        $allOk = $false
    }
} catch {
    Write-Host "  [ERROR] Python not installed" -ForegroundColor Red
    Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Yellow
    $allOk = $false
}

# 6. Git Check
Write-Host "`n[6/8] Checking Git..." -ForegroundColor Yellow
try {
    $gitVersion = git --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] $gitVersion installed" -ForegroundColor Green
    } else {
        Write-Host "  [WARNING] Git not installed" -ForegroundColor Yellow
        Write-Host "     Install: .\scripts\install-prerequisites.ps1" -ForegroundColor Cyan
        $warnings += "Git installation recommended"
    }
} catch {
    Write-Host "  [WARNING] Git check failed" -ForegroundColor Yellow
    $warnings += "Git installation recommended"
}

# 7. Gradle Wrapper Check
Write-Host "`n[7/8] Checking Gradle Wrapper..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    if (Test-Path "gradle\wrapper\gradle-wrapper.jar") {
        Write-Host "  [OK] Gradle Wrapper files exist" -ForegroundColor Green
        try {
            $gradleOutput = .\gradlew.bat -v 2>&1 | Out-String
            if ($gradleOutput -match "Gradle\s+(\d+\.\d+)") {
                $gradleVersion = $matches[1]
                Write-Host "  [OK] Gradle $gradleVersion available" -ForegroundColor Green
            }
        } catch {
            Write-Host "  [WARNING] Gradle Wrapper execution check needed" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  [WARNING] gradle-wrapper.jar not found" -ForegroundColor Yellow
        Write-Host "     Run: .\scripts\setup-gradle-wrapper.ps1" -ForegroundColor Cyan
        $warnings += "Gradle Wrapper setup needed"
    }
} else {
    Write-Host "  [WARNING] gradlew.bat not found" -ForegroundColor Yellow
}

# 8. Port Usage Check
Write-Host "`n[8/8] Checking port usage..." -ForegroundColor Yellow
$ports = @(
    @{Port = 3306; Service = "MariaDB"},
    @{Port = 6379; Service = "Redis"},
    @{Port = 8083; Service = "Spring Boot (local)"},
    @{Port = 8084; Service = "Spring Boot (agent)"},
    @{Port = 8000; Service = "AI Service"}
)

foreach ($portInfo in $ports) {
    $port = $portInfo.Port
    $service = $portInfo.Service
    $connection = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connection) {
        $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
        $processName = if ($process) { $process.ProcessName } else { "Unknown" }
        Write-Host "  [WARNING] Port $port ($service) in use: $processName" -ForegroundColor Yellow
        $warnings += "Port $port in use"
    } else {
        Write-Host "  [OK] Port $port ($service) available" -ForegroundColor Green
    }
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
if ($allOk) {
    Write-Host "  [OK] All required items are ready!" -ForegroundColor Green
    
    if ($warnings.Count -gt 0) {
        Write-Host "`n[WARNING] Warnings:" -ForegroundColor Yellow
        foreach ($warning in $warnings) {
            Write-Host "  - $warning" -ForegroundColor Yellow
        }
    }
    
    Write-Host "`nNext Steps:" -ForegroundColor Cyan
    Write-Host "  1. Run full setup: .\scripts\setup-local-complete.ps1" -ForegroundColor White
    Write-Host "  2. Or manual setup: docs\08-setup-guides\01-local-setup-complete.md" -ForegroundColor White
} else {
    Write-Host "  [ERROR] Some required items are missing" -ForegroundColor Red
    Write-Host "`nAuto-install available:" -ForegroundColor Cyan
    Write-Host "  .\scripts\install-prerequisites.ps1" -ForegroundColor White
    Write-Host "`nOr manual setup guide:" -ForegroundColor Cyan
    Write-Host "  docs\08-setup-guides\01-local-setup-complete.md" -ForegroundColor White
}
Write-Host "========================================`n" -ForegroundColor Cyan

exit $(if ($allOk) { 0 } else { 1 })
