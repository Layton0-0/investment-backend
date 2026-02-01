# Investment Choi Prerequisites Auto-Install Script
# Usage: .\scripts\install-prerequisites.ps1
# Automatically installs WSL2, Docker Desktop, Java, Python, Git

param(
    [switch]$SkipWSL,
    [switch]$SkipDocker,
    [switch]$SkipJava,
    [switch]$SkipPython,
    [switch]$SkipGit
)

# Check if running as administrator
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)

if (-not $isAdmin) {
    Write-Host "[WARNING] This script requires administrator privileges." -ForegroundColor Yellow
    Write-Host "Some installations (WSL2) may require admin rights." -ForegroundColor Yellow
    Write-Host "Continue anyway? (Y/N)" -ForegroundColor Cyan
    $response = Read-Host
    if ($response -ne "Y" -and $response -ne "y") {
        exit 1
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Investment Choi Prerequisites Installer" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$needsReboot = $false
$installCount = 0

# Check winget availability
Write-Host "[0/5] Checking winget..." -ForegroundColor Yellow
try {
    $wingetVersion = winget --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] winget available: $wingetVersion" -ForegroundColor Green
    } else {
        Write-Host "  [ERROR] winget not found. Please install Windows App Installer from Microsoft Store." -ForegroundColor Red
        Write-Host "  URL: https://aka.ms/getwinget" -ForegroundColor Cyan
        exit 1
    }
} catch {
    Write-Host "  [ERROR] winget not found. Please install Windows App Installer from Microsoft Store." -ForegroundColor Red
    Write-Host "  URL: https://aka.ms/getwinget" -ForegroundColor Cyan
    exit 1
}

# 1. WSL2 Installation
if (-not $SkipWSL) {
    Write-Host "`n[1/5] Installing WSL2..." -ForegroundColor Yellow
    
    try {
        $wslVersion = wsl --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            $wslList = wsl --list --verbose 2>&1
            if ($wslList -match "VERSION\s+2") {
                Write-Host "  [OK] WSL2 already installed" -ForegroundColor Green
            } else {
                Write-Host "  Installing WSL2..." -ForegroundColor Cyan
                if ($isAdmin) {
                    wsl --install --no-distribution
                    if ($LASTEXITCODE -eq 0) {
                        Write-Host "  [OK] WSL2 installation initiated" -ForegroundColor Green
                        Write-Host "  [WARNING] Reboot required. Please reboot and run this script again." -ForegroundColor Yellow
                        $needsReboot = $true
                        $installCount++
                    } else {
                        Write-Host "  [ERROR] WSL2 installation failed" -ForegroundColor Red
                    }
                } else {
                    Write-Host "  [ERROR] Administrator privileges required for WSL2 installation" -ForegroundColor Red
                    Write-Host "  Please run: wsl --install (as administrator)" -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "  Installing WSL..." -ForegroundColor Cyan
            if ($isAdmin) {
                wsl --install --no-distribution
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "  [OK] WSL installation initiated" -ForegroundColor Green
                    Write-Host "  [WARNING] Reboot required. Please reboot and run this script again." -ForegroundColor Yellow
                    $needsReboot = $true
                    $installCount++
                } else {
                    Write-Host "  [ERROR] WSL installation failed" -ForegroundColor Red
                }
            } else {
                Write-Host "  [ERROR] Administrator privileges required for WSL installation" -ForegroundColor Red
                Write-Host "  Please run: wsl --install (as administrator)" -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "  [ERROR] WSL check failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "`n[1/5] Skipping WSL2 installation (--SkipWSL)" -ForegroundColor Yellow
}

# 2. Docker Desktop Installation
if (-not $SkipDocker) {
    Write-Host "`n[2/5] Installing Docker Desktop..." -ForegroundColor Yellow
    
    try {
        $dockerVersion = docker --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Docker already installed: $dockerVersion" -ForegroundColor Green
        } else {
            Write-Host "  Installing Docker Desktop via winget..." -ForegroundColor Cyan
            Write-Host "  This may take several minutes. Please wait..." -ForegroundColor Yellow
            
            # Try silent install first
            $installResult = winget install --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements --silent 2>&1
            $installExitCode = $LASTEXITCODE
            
            if ($installExitCode -eq 0) {
                Write-Host "  [OK] Docker Desktop installation completed" -ForegroundColor Green
                Write-Host "  [INFO] Please start Docker Desktop manually and ensure WSL2 backend is enabled." -ForegroundColor Cyan
                $installCount++
            } else {
                # If silent install fails, try interactive install
                Write-Host "  Silent install failed. Trying interactive install..." -ForegroundColor Yellow
                $installResult = winget install --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements 2>&1
                $installExitCode = $LASTEXITCODE
                
                if ($installExitCode -eq 0) {
                    Write-Host "  [OK] Docker Desktop installation completed" -ForegroundColor Green
                    Write-Host "  [INFO] Please start Docker Desktop manually and ensure WSL2 backend is enabled." -ForegroundColor Cyan
                    $installCount++
                } else {
                    Write-Host "  [ERROR] Docker Desktop installation failed" -ForegroundColor Red
                    Write-Host "  Error output: $installResult" -ForegroundColor Yellow
                    Write-Host "  Please install manually from: https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
                }
            }
        }
    } catch {
        Write-Host "  [ERROR] Docker installation failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "`n[2/5] Skipping Docker Desktop installation (--SkipDocker)" -ForegroundColor Yellow
}

# 3. Java Installation
if (-not $SkipJava) {
    Write-Host "`n[3/5] Installing Java 17..." -ForegroundColor Yellow
    
    try {
        $javaOutput = java -version 2>&1 | Out-String
        if ($javaOutput -match "version\s+""?(\d+)") {
            $javaVersion = $matches[1]
            if ([int]$javaVersion -ge 17) {
                Write-Host "  [OK] Java $javaVersion already installed" -ForegroundColor Green
            } else {
                Write-Host "  Installing Java 17 via winget..." -ForegroundColor Cyan
                winget install --id EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements --silent
                if ($LASTEXITCODE -eq 0) {
                    Write-Host "  [OK] Java 17 installation completed" -ForegroundColor Green
                    Write-Host "  [INFO] Please restart your terminal for Java to be available in PATH." -ForegroundColor Cyan
                    $installCount++
                } else {
                    Write-Host "  [WARNING] Java installation may have failed. Check manually." -ForegroundColor Yellow
                }
            }
        } else {
            Write-Host "  Installing Java 17 via winget..." -ForegroundColor Cyan
            winget install --id EclipseAdoptium.Temurin.17.JDK --accept-package-agreements --accept-source-agreements --silent
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  [OK] Java 17 installation completed" -ForegroundColor Green
                Write-Host "  [INFO] Please restart your terminal for Java to be available in PATH." -ForegroundColor Cyan
                $installCount++
            } else {
                Write-Host "  [WARNING] Java installation may have failed. Check manually." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "  [ERROR] Java installation failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "`n[3/5] Skipping Java installation (--SkipJava)" -ForegroundColor Yellow
}

# 4. Python Installation
if (-not $SkipPython) {
    Write-Host "`n[4/5] Installing Python 3.11..." -ForegroundColor Yellow
    
    try {
        $pythonVersion = python --version 2>&1
        if ($pythonVersion -match "Python\s+3\.(1[1-9]|[2-9][0-9])") {
            Write-Host "  [OK] $pythonVersion already installed" -ForegroundColor Green
        } else {
            Write-Host "  Installing Python 3.11 via winget..." -ForegroundColor Cyan
            winget install --id Python.Python.3.11 --accept-package-agreements --accept-source-agreements --silent
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  [OK] Python 3.11 installation completed" -ForegroundColor Green
                Write-Host "  [INFO] Please restart your terminal for Python to be available in PATH." -ForegroundColor Cyan
                $installCount++
            } else {
                Write-Host "  [WARNING] Python installation may have failed. Check manually." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "  [ERROR] Python installation failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "`n[4/5] Skipping Python installation (--SkipPython)" -ForegroundColor Yellow
}

# 5. Git Installation (optional but recommended)
if (-not $SkipGit) {
    Write-Host "`n[5/5] Installing Git..." -ForegroundColor Yellow
    
    try {
        $gitVersion = git --version 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] $gitVersion already installed" -ForegroundColor Green
        } else {
            Write-Host "  Installing Git via winget..." -ForegroundColor Cyan
            winget install --id Git.Git --accept-package-agreements --accept-source-agreements --silent
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  [OK] Git installation completed" -ForegroundColor Green
                Write-Host "  [INFO] Please restart your terminal for Git to be available in PATH." -ForegroundColor Cyan
                $installCount++
            } else {
                Write-Host "  [WARNING] Git installation may have failed. Check manually." -ForegroundColor Yellow
            }
        }
    } catch {
        Write-Host "  [ERROR] Git installation failed: $_" -ForegroundColor Red
    }
} else {
    Write-Host "`n[5/5] Skipping Git installation (--SkipGit)" -ForegroundColor Yellow
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Installation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

if ($installCount -gt 0) {
    Write-Host "`n[OK] $installCount package(s) installed or updated" -ForegroundColor Green
} else {
    Write-Host "`n[OK] All prerequisites are already installed" -ForegroundColor Green
}

if ($needsReboot) {
    Write-Host "`n[IMPORTANT] Reboot required for WSL2!" -ForegroundColor Yellow
    Write-Host "  After reboot, run this script again to continue:" -ForegroundColor Cyan
    Write-Host "    .\scripts\install-prerequisites.ps1" -ForegroundColor White
    Write-Host "  Or run the full setup:" -ForegroundColor Cyan
    Write-Host "    .\scripts\setup-local-complete.ps1" -ForegroundColor White
} else {
    Write-Host "`nNext Steps:" -ForegroundColor Cyan
    Write-Host "  1. If Docker Desktop was installed, start it manually" -ForegroundColor White
    Write-Host "  2. Restart your terminal (for PATH updates)" -ForegroundColor White
    Write-Host "  3. Run the full setup:" -ForegroundColor White
    Write-Host "     .\scripts\setup-local-complete.ps1" -ForegroundColor Yellow
}

Write-Host "========================================`n" -ForegroundColor Cyan
