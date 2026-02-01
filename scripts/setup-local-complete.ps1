# Investment Choi Local Development Environment Setup Script
# Usage: .\scripts\setup-local-complete.ps1
# 실행 정책 오류 시: .\scripts\setup-local-complete.cmd 또는 Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
# Automates full setup using WSL2 + Docker Compose

param(
    [switch]$SkipPrerequisites,
    [switch]$SkipDocker,
    [switch]$SkipPython,
    [switch]$SkipEnv
)

$ErrorActionPreference = "Continue"
$ProjectRoot = $PSScriptRoot | Split-Path -Parent

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Investment Choi Local Environment Setup" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Prerequisites Check and Auto-Install
if (-not $SkipPrerequisites) {
    Write-Host "[1/7] Checking and installing prerequisites..." -ForegroundColor Yellow
    
    # Check if running as administrator
    $isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    
    $needsReboot = $false
    $installCount = 0
    
    # Check winget
    try {
        $wingetVersion = winget --version 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [ERROR] winget not found. Please install Windows App Installer from Microsoft Store." -ForegroundColor Red
            Write-Host "  URL: https://aka.ms/getwinget" -ForegroundColor Cyan
            exit 1
        }
    } catch {
        Write-Host "  [ERROR] winget not found. Please install Windows App Installer from Microsoft Store." -ForegroundColor Red
        Write-Host "  URL: https://aka.ms/getwinget" -ForegroundColor Cyan
        exit 1
    }
    
    # Check and install WSL2
    try {
        $wslVersion = wsl --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $wslList = wsl --list --verbose 2>&1
            if ($wslList -match "VERSION\s+2") {
                Write-Host "  [OK] WSL2 installed" -ForegroundColor Green
            } else {
                Write-Host "  Installing WSL2..." -ForegroundColor Cyan
                if ($isAdmin) {
                    wsl --install --no-distribution 2>&1 | Out-Null
                    if ($LASTEXITCODE -eq 0) {
                        Write-Host "  [OK] WSL2 installation initiated" -ForegroundColor Green
                        Write-Host "  [WARNING] Reboot required. Please reboot and run this script again." -ForegroundColor Yellow
                        $needsReboot = $true
                        $installCount++
                    }
                } else {
                    Write-Host "  [WARNING] Administrator privileges required for WSL2. Skipping..." -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "  Installing WSL..." -ForegroundColor Cyan
            if ($isAdmin) {
                wsl --install --no-distribution 2>&1 | Out-Null
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "  [OK] WSL installation initiated" -ForegroundColor Green
                    Write-Host "  [WARNING] Reboot required. Please reboot and run this script again." -ForegroundColor Yellow
                    $needsReboot = $true
                    $installCount++
                }
            } else {
                Write-Host "  [WARNING] Administrator privileges required for WSL. Skipping..." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "  [WARNING] WSL check failed: $_" -ForegroundColor Yellow
    }
    
    # Check and install Docker Desktop
    $dockerInstalled = $false
    try {
        $dockerVersion = docker --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Docker installed: $dockerVersion" -ForegroundColor Green
            $dockerInstalled = $true
        }
    } catch {
        # Docker not found, will install
    }
    
    if (-not $dockerInstalled) {
        Write-Host "  Installing Docker Desktop via winget..." -ForegroundColor Cyan
        Write-Host "  This may take several minutes. Please wait..." -ForegroundColor Yellow
        
        # Try silent install first
        $installResult = winget install --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements --silent 2>&1
        $installExitCode = $LASTEXITCODE
        
        if ($installExitCode -eq 0) {
            Write-Host "  [OK] Docker Desktop installation completed" -ForegroundColor Green
            Write-Host "  [INFO] Please start Docker Desktop manually and ensure WSL2 backend is enabled." -ForegroundColor Cyan
            Write-Host "  [INFO] After starting Docker Desktop, restart your terminal and run this script again." -ForegroundColor Cyan
            $installCount++
            $needsReboot = $true
        } else {
            # If silent install fails, try interactive install
            Write-Host "  Silent install failed. Trying interactive install..." -ForegroundColor Yellow
            $installResult = winget install --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements 2>&1
            $installExitCode = $LASTEXITCODE
            
            if ($installExitCode -eq 0) {
                Write-Host "  [OK] Docker Desktop installation completed" -ForegroundColor Green
                Write-Host "  [INFO] Please start Docker Desktop manually and ensure WSL2 backend is enabled." -ForegroundColor Cyan
                Write-Host "  [INFO] After starting Docker Desktop, restart your terminal and run this script again." -ForegroundColor Cyan
                $installCount++
                $needsReboot = $true
            } else {
                Write-Host "  [ERROR] Docker Desktop installation failed" -ForegroundColor Red
                Write-Host "  Error: $installResult" -ForegroundColor Yellow
                Write-Host "  Please install manually from: https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
            }
        }
    }
    
    # Check and install Java (check existence first so "java" not in PATH does not throw)
    $javaCmd = Get-Command java -ErrorAction SilentlyContinue
    if (-not $javaCmd) {
        Write-Host "  Java not found. Installing Java 17 via winget..." -ForegroundColor Cyan
        winget install --id EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements --silent 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Java 17 installation completed" -ForegroundColor Green
            $installCount++
        } else {
            Write-Host "  [WARNING] Java 17 winget install failed (exit $LASTEXITCODE). Install manually: EclipseAdoptium.Temurin.17.JDK" -ForegroundColor Yellow
        }
    } else {
        try {
            $javaOutput = java -version 2>&1 | Out-String
            if ($javaOutput -match "version\s+""?(\d+)") {
                $javaVersion = $matches[1]
                if ([int]$javaVersion -ge 17) {
                    Write-Host "  [OK] Java $javaVersion installed" -ForegroundColor Green
                } else {
                    Write-Host "  Installing Java 17 via winget (current $javaVersion)..." -ForegroundColor Cyan
                    winget install --id EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements --silent 2>&1 | Out-Null
                    if ($LASTEXITCODE -eq 0) {
                        Write-Host "  [OK] Java 17 installation completed" -ForegroundColor Green
                        $installCount++
                    }
                }
            } else {
                Write-Host "  [OK] Java installed" -ForegroundColor Green
            }
        } catch {
            Write-Host "  [WARNING] Java version check failed: $_" -ForegroundColor Yellow
        }
    }
    
    # Check and install Python
    try {
        $pythonVersion = python --version 2>&1
        if ($pythonVersion -match "Python\s+3\.(1[1-9]|[2-9][0-9])") {
            Write-Host "  [OK] $pythonVersion installed" -ForegroundColor Green
        } else {
            Write-Host "  Installing Python 3.11 via winget..." -ForegroundColor Cyan
            winget install --id Python.Python.3.11 --accept-package-agreements --accept-source-agreements --silent 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  [OK] Python 3.11 installation completed" -ForegroundColor Green
                $installCount++
            }
        }
    } catch {
        Write-Host "  [WARNING] Python check/install failed: $_" -ForegroundColor Yellow
    }
    
    # Check and install Git (optional)
    try {
        $gitVersion = git --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] $gitVersion installed" -ForegroundColor Green
        } else {
            Write-Host "  Installing Git via winget..." -ForegroundColor Cyan
            winget install --id Git.Git --accept-package-agreements --accept-source-agreements --silent 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  [OK] Git installation completed" -ForegroundColor Green
                $installCount++
            }
        }
    } catch {
        Write-Host "  [WARNING] Git check/install failed: $_" -ForegroundColor Yellow
    }
    
    if ($needsReboot) {
        Write-Host "`n[IMPORTANT] Action required!" -ForegroundColor Yellow
        if ($installCount -gt 0) {
            Write-Host "  $installCount package(s) installed." -ForegroundColor Green
        }
        Write-Host "  Please:" -ForegroundColor Cyan
        Write-Host "    1. Start Docker Desktop (if installed)" -ForegroundColor White
        Write-Host "    2. Restart your terminal for PATH updates" -ForegroundColor White
        Write-Host "    3. Run this script again: .\scripts\setup-local-complete.ps1" -ForegroundColor White
        exit 0
    }
    
    if ($installCount -gt 0) {
        Write-Host "`n[OK] $installCount package(s) installed. Please restart your terminal for PATH updates." -ForegroundColor Green
        Write-Host "  Then run this script again to continue." -ForegroundColor Cyan
        exit 0
    }
    
    Write-Host "`n[OK] All prerequisites are ready`n" -ForegroundColor Green
} else {
    Write-Host "[1/7] Skipping prerequisites check (--SkipPrerequisites)" -ForegroundColor Yellow
}

# 2. Gradle Wrapper Setup
Write-Host "[2/7] Setting up Gradle Wrapper..." -ForegroundColor Yellow
if (-not (Test-Path "$ProjectRoot\gradle\wrapper\gradle-wrapper.jar")) {
    Write-Host "  Downloading Gradle Wrapper JAR..." -ForegroundColor Cyan
    & "$PSScriptRoot\setup-gradle-wrapper.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARNING] Gradle Wrapper setup failed. Please run manually: .\scripts\setup-gradle-wrapper.ps1" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] Gradle Wrapper setup completed" -ForegroundColor Green
    }
} else {
    Write-Host "  [OK] Gradle Wrapper already configured" -ForegroundColor Green
}

# 3. Docker Compose Infrastructure
if (-not $SkipDocker) {
    Write-Host "`n[3/7] Starting Docker Compose infrastructure..." -ForegroundColor Yellow
    
    # Check docker-compose.yml
    if (-not (Test-Path "$ProjectRoot\docker-compose.yml")) {
        Write-Host "  [ERROR] docker-compose.yml not found." -ForegroundColor Red
        exit 1
    }
    
    # Check Docker
    try {
        $dockerPs = docker ps 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Host "  [ERROR] Docker Desktop is not running." -ForegroundColor Red
            Write-Host "     Please start Docker Desktop and try again." -ForegroundColor Yellow
            exit 1
        }
    } catch {
        Write-Host "  [ERROR] Docker check failed: $_" -ForegroundColor Red
        exit 1
    }
    
    # Check existing containers
    Write-Host "  Checking existing containers..." -ForegroundColor Cyan
    try {
        $existingOutput = docker compose ps --format json 2>&1
        if ($existingOutput) {
            $existingContainers = $existingOutput | ConvertFrom-Json -ErrorAction SilentlyContinue
            if ($existingContainers) {
                Write-Host "  Existing containers found. Restarting..." -ForegroundColor Cyan
                docker compose down 2>&1 | Out-Null
            }
        }
    } catch {
        # Ignore and continue
    }
    
    # Start Docker Compose
    Write-Host "  Starting Docker Compose..." -ForegroundColor Cyan
    Set-Location $ProjectRoot
    docker compose up -d
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Docker Compose started" -ForegroundColor Green
        
        # Check container status (wait up to 60 seconds)
        Write-Host "  Checking container status..." -ForegroundColor Cyan
        $maxWait = 60
        $elapsed = 0
        $allHealthy = $false
        
        while ($elapsed -lt $maxWait -and -not $allHealthy) {
            Start-Sleep -Seconds 2
            $elapsed += 2
            
            try {
                $mariadbOutput = docker compose ps mariadb --format json 2>&1
                $redisOutput = docker compose ps redis --format json 2>&1
                
                if ($mariadbOutput -and $redisOutput) {
                    $mariadb = $mariadbOutput | ConvertFrom-Json -ErrorAction SilentlyContinue
                    $redis = $redisOutput | ConvertFrom-Json -ErrorAction SilentlyContinue
                    
                    if ($mariadb -and $redis -and $mariadb.State -eq "running" -and $redis.State -eq "running") {
                        $allHealthy = $true
                    }
                }
            } catch {
                # Continue waiting
            }
        }
        
        if ($allHealthy) {
            Write-Host "  [OK] All containers running" -ForegroundColor Green
        } else {
            Write-Host "  [WARNING] Some containers are still starting. Check logs: docker compose logs" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  [ERROR] Docker Compose start failed" -ForegroundColor Red
        Write-Host "     Check logs: docker compose logs" -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "`n[3/7] Skipping Docker Compose (--SkipDocker)" -ForegroundColor Yellow
}

# 4. Python Virtual Environment
if (-not $SkipPython) {
    Write-Host "`n[4/7] Setting up Python virtual environment..." -ForegroundColor Yellow
    & "$PSScriptRoot\setup-python-env.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  [WARNING] Python virtual environment setup failed. Please run manually: .\scripts\setup-python-env.ps1" -ForegroundColor Yellow
    } else {
        Write-Host "  [OK] Python virtual environment setup completed" -ForegroundColor Green
    }
} else {
    Write-Host "`n[4/7] Skipping Python virtual environment (--SkipPython)" -ForegroundColor Yellow
}

# 5. .env File Template
if (-not $SkipEnv) {
    Write-Host "`n[5/7] Setting up environment variables file..." -ForegroundColor Yellow
    
    $envFile = "$ProjectRoot\.env"
    $envTemplate = "$ProjectRoot\.env.template"
    
    if (-not (Test-Path $envFile)) {
        if (Test-Path $envTemplate) {
            Write-Host "  Creating .env file from .env.template..." -ForegroundColor Cyan
            Copy-Item $envTemplate -Destination $envFile
            Write-Host "  [OK] .env file created" -ForegroundColor Green
            Write-Host "  [WARNING] Please edit .env file and enter required values (API keys, passwords, etc.)" -ForegroundColor Yellow
        } else {
            Write-Host "  [WARNING] .env.template not found. Please create .env file manually." -ForegroundColor Yellow
        }
    } else {
        Write-Host "  [OK] .env file already exists" -ForegroundColor Green
    }
} else {
    Write-Host "`n[5/7] Skipping environment variables file (--SkipEnv)" -ForegroundColor Yellow
}

# 6. MCP Installation and Configuration
Write-Host "`n[6/7] Installing and configuring MCP servers..." -ForegroundColor Yellow
& "$PSScriptRoot\setup-mcp.ps1"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  [WARNING] MCP installation may have failed. Please run manually: .\scripts\setup-mcp.ps1" -ForegroundColor Yellow
} else {
    Write-Host "  [OK] MCP installation completed" -ForegroundColor Green
}

# 7. Final Status Check and Report
Write-Host "`n[7/7] Checking final status..." -ForegroundColor Yellow

$report = @{
    Docker = $false
    Python = $false
    Gradle = $false
    Env = $false
}

# Docker Check
try {
    $dockerOutput = docker compose ps --format json 2>&1
    if ($dockerOutput) {
        $dockerPs = $dockerOutput | ConvertFrom-Json -ErrorAction SilentlyContinue
        if ($dockerPs) {
            # Handle both array and single object cases
            if ($dockerPs -is [Array]) {
                $mariadb = $dockerPs | Where-Object { $_.Service -eq "mariadb" -and $_.State -eq "running" }
                $redis = $dockerPs | Where-Object { $_.Service -eq "redis" -and $_.State -eq "running" }
            } else {
                if ($dockerPs.Service -eq "mariadb" -and $dockerPs.State -eq "running") {
                    $mariadb = $dockerPs
                }
                if ($dockerPs.Service -eq "redis" -and $dockerPs.State -eq "running") {
                    $redis = $dockerPs
                }
            }
            if ($mariadb -and $redis) {
                $report.Docker = $true
            }
        }
    }
} catch { }

# Python Check
$venvPath = "$ProjectRoot\ai-service\prediction-service\venv"
if (Test-Path $venvPath) {
    $venvPython = Join-Path $venvPath "Scripts\python.exe"
    if (Test-Path $venvPython) {
        try {
            $fastapiCheck = & $venvPython -c "import fastapi; print('OK')" 2>&1
            if ($fastapiCheck -eq "OK") {
                $report.Python = $true
            }
        } catch { }
    }
}

# Gradle Check
if (Test-Path "$ProjectRoot\gradle\wrapper\gradle-wrapper.jar") {
    $report.Gradle = $true
}

# .env Check
if (Test-Path "$ProjectRoot\.env") {
    $report.Env = $true
}

# Report Output
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Setup Completion Report" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nSetup Status:" -ForegroundColor Yellow
Write-Host "  Docker Compose: $(if ($report.Docker) { '[OK]' } else { '[NOT RUNNING]' })" -ForegroundColor $(if ($report.Docker) { "Green" } else { "Red" })
Write-Host "  Python Virtual Env: $(if ($report.Python) { '[OK]' } else { '[NOT SETUP]' })" -ForegroundColor $(if ($report.Python) { "Green" } else { "Red" })
Write-Host "  Gradle Wrapper: $(if ($report.Gradle) { '[OK]' } else { '[NOT SETUP]' })" -ForegroundColor $(if ($report.Gradle) { "Green" } else { "Red" })
Write-Host "  .env File: $(if ($report.Env) { '[OK]' } else { '[NOT FOUND]' })" -ForegroundColor $(if ($report.Env) { "Green" } else { "Red" })

Write-Host "`nNext Steps:" -ForegroundColor Cyan
Write-Host "  1. Edit .env file (API keys, passwords, etc.)" -ForegroundColor White
$mcpConfigPath = "$env:USERPROFILE\.cursor\mcp.json"
Write-Host "  2. Edit MCP configuration file (GitHub/Notion tokens): $mcpConfigPath" -ForegroundColor White
Write-Host "  3. Run Spring Boot:" -ForegroundColor White
Write-Host "     .\gradlew.bat bootRun" -ForegroundColor Yellow
Write-Host "  4. Run AI Service (new terminal):" -ForegroundColor White
Write-Host "     cd ai-service\prediction-service" -ForegroundColor Yellow
Write-Host "     .\venv\Scripts\Activate.ps1" -ForegroundColor Yellow
Write-Host "     uvicorn app.main:app --reload --host 0.0.0.0 --port 8000" -ForegroundColor Yellow
Write-Host "`nDetailed Guide:" -ForegroundColor Cyan
Write-Host "  docs\08-setup-guides\01-local-setup-complete.md" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan
