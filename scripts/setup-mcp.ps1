# Investment Choi MCP Installation Script
# Usage: .\scripts\setup-mcp.ps1
# Installs and configures MCP servers

$ErrorActionPreference = "Continue"
$ProjectRoot = $PSScriptRoot | Split-Path -Parent

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  Investment Choi MCP Installation" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. Check Node.js/npm
Write-Host "[1/7] Checking Node.js..." -ForegroundColor Yellow
try {
    $nodeVersion = node --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Node.js installed: $nodeVersion" -ForegroundColor Green
    } else {
        Write-Host "  Installing Node.js via winget..." -ForegroundColor Cyan
        winget install --id OpenJS.NodeJS.LTS --accept-package-agreements --accept-source-agreements --silent 2>&1 | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] Node.js installation completed" -ForegroundColor Green
            Write-Host "  [INFO] Please restart your terminal for Node.js to be available in PATH." -ForegroundColor Cyan
            exit 0
        } else {
            Write-Host "  [ERROR] Node.js installation failed" -ForegroundColor Red
            Write-Host "  Please install manually from: https://nodejs.org/" -ForegroundColor Cyan
            exit 1
        }
    }
} catch {
    Write-Host "  Installing Node.js via winget..." -ForegroundColor Cyan
    winget install --id OpenJS.NodeJS.LTS --accept-package-agreements --accept-source-agreements --silent 2>&1 | Out-Null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] Node.js installation completed" -ForegroundColor Green
        Write-Host "  [INFO] Please restart your terminal for Node.js to be available in PATH." -ForegroundColor Cyan
        exit 0
    } else {
        Write-Host "  [ERROR] Node.js installation failed" -ForegroundColor Red
        exit 1
    }
}

# 2. Check Python/uv for MariaDB MCP
Write-Host "`n[2/7] Checking Python/uv for MariaDB MCP..." -ForegroundColor Yellow
try {
    $uvVersion = uv --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  [OK] uv installed: $uvVersion" -ForegroundColor Green
    } else {
        Write-Host "  Installing uv..." -ForegroundColor Cyan
        # Install uv via pip or direct download
        $pythonVersion = python --version 2>&1
        if ($pythonVersion -match "Python\s+3\.(1[1-9]|[2-9][0-9])") {
            pip install uv 2>&1 | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  [OK] uv installation completed" -ForegroundColor Green
            } else {
                Write-Host "  [WARNING] uv installation failed. MariaDB MCP may not work." -ForegroundColor Yellow
            }
        } else {
            Write-Host "  [WARNING] Python not found. MariaDB MCP requires Python 3.11+." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  [WARNING] uv check failed. MariaDB MCP may not work." -ForegroundColor Yellow
}

# 3. Install MCP packages via npm (pre-install for faster startup)
Write-Host "`n[3/7] Pre-installing MCP packages..." -ForegroundColor Yellow

Write-Host "  Installing @modelcontextprotocol/server-filesystem..." -ForegroundColor Cyan
npm install -g @modelcontextprotocol/server-filesystem 2>&1 | Out-Null

Write-Host "  Installing @modelcontextprotocol/server-github..." -ForegroundColor Cyan
npm install -g @modelcontextprotocol/server-github 2>&1 | Out-Null

Write-Host "  Installing @notionhq/notion-mcp-server..." -ForegroundColor Cyan
npm install -g @notionhq/notion-mcp-server 2>&1 | Out-Null

Write-Host "  [OK] MCP packages pre-installed" -ForegroundColor Green

# 4. Create MCP configuration file (if not exists)
Write-Host "`n[4/7] Checking MCP configuration file..." -ForegroundColor Yellow

$mcpConfigPath = "$env:USERPROFILE\.cursor\mcp.json"
$mcpTemplatePath = "$ProjectRoot\.cursor\mcp.json.template"

if (-not (Test-Path $mcpConfigPath)) {
    if (Test-Path $mcpTemplatePath) {
        Write-Host "  Creating MCP configuration directory..." -ForegroundColor Cyan
        $mcpDir = Split-Path $mcpConfigPath -Parent
        if (-not (Test-Path $mcpDir)) {
            New-Item -ItemType Directory -Path $mcpDir -Force | Out-Null
        }
        
        Write-Host "  Copying MCP configuration template..." -ForegroundColor Cyan
        try {
            $templateContent = Get-Content $mcpTemplatePath -Raw -Encoding UTF8
        } catch {
            $templateContent = [System.IO.File]::ReadAllText($mcpTemplatePath, [System.Text.Encoding]::UTF8)
        }
        
        # Auto-configure project path (Windows path)
        $projectPathWin = $ProjectRoot -replace '/', '\'
        $projectPathUnix = $ProjectRoot -replace '\\', '/'
        $templateContent = $templateContent -replace 'D:/works/pjt/investment-choi', $projectPathUnix
        
        # Save to user path
        try {
            $templateContent | Set-Content $mcpConfigPath -Encoding UTF8
        } catch {
            [System.IO.File]::WriteAllText($mcpConfigPath, $templateContent, [System.Text.Encoding]::UTF8)
        }
        
        Write-Host "  [OK] MCP configuration file created: $mcpConfigPath" -ForegroundColor Green
    } else {
        Write-Host "  [WARNING] MCP configuration template not found." -ForegroundColor Yellow
    }
} else {
    Write-Host "  [OK] MCP configuration file already exists" -ForegroundColor Green
}

# 5. Check environment variables
Write-Host "`n[5/7] Checking environment variables..." -ForegroundColor Yellow

$githubToken = [System.Environment]::GetEnvironmentVariable("GITHUB_TOKEN", "User")
$notionToken = [System.Environment]::GetEnvironmentVariable("NOTION_TOKEN", "User")

if ($githubToken) {
    Write-Host "  [OK] GITHUB_TOKEN is set" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] GITHUB_TOKEN not set. GitHub MCP will not work." -ForegroundColor Yellow
    Write-Host "    Set it with: [System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', 'your_token', 'User')" -ForegroundColor Cyan
}

if ($notionToken) {
    Write-Host "  [OK] NOTION_TOKEN is set" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] NOTION_TOKEN not set. Notion MCP will not work." -ForegroundColor Yellow
    Write-Host "    Set it with: [System.Environment]::SetEnvironmentVariable('NOTION_TOKEN', 'your_token', 'User')" -ForegroundColor Cyan
}

# 6. Install MariaDB MCP (if uv is available)
Write-Host "`n[6/7] Installing MariaDB MCP..." -ForegroundColor Yellow

$uvInstalled = $false
try {
    $uvVersion = uv --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        $uvInstalled = $true
    }
} catch {
    # uv not available
}

if ($uvInstalled) {
    try {
        Write-Host "  Installing mcp-server-mariadb via uv..." -ForegroundColor Cyan
        $installOutput = uv tool install mcp-server-mariadb 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Host "  [OK] MariaDB MCP installed" -ForegroundColor Green
        } else {
            Write-Host "  [WARNING] MariaDB MCP installation failed" -ForegroundColor Yellow
            Write-Host "    Error: $installOutput" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "  [WARNING] MariaDB MCP installation failed: $_" -ForegroundColor Yellow
    }
} else {
    Write-Host "  [SKIP] uv not available. Skipping MariaDB MCP installation." -ForegroundColor Yellow
    Write-Host "    Install uv manually: pip install uv" -ForegroundColor Cyan
}

# 7. Fix MCP configuration (merge template with existing, set correct paths)
Write-Host "`n[7/7] Fixing MCP configuration..." -ForegroundColor Yellow

# Get project path
$projectPathWin = $ProjectRoot -replace '/', '\'
$projectPathUnix = $ProjectRoot -replace '\\', '/'

# Read existing config
$existingConfig = @{}
if (Test-Path $mcpConfigPath) {
    try {
        $existingContent = Get-Content $mcpConfigPath -Raw -Encoding UTF8
        $existingConfig = $existingContent | ConvertFrom-Json
        Write-Host "  Found existing MCP config" -ForegroundColor Green
    } catch {
        Write-Host "  Warning: Could not parse existing config" -ForegroundColor Yellow
    }
}

# Read template
if (Test-Path $mcpTemplatePath) {
    $templateContent = Get-Content $mcpTemplatePath -Raw -Encoding UTF8
    $templateConfig = $templateContent | ConvertFrom-Json
    
    # Replace project path in template
    $templateContent = $templateContent -replace 'D:/works/pjt/investment-choi', $projectPathUnix
    $templateConfig = $templateContent | ConvertFrom-Json
    
    # Find mcp-server-mariadb.exe path
    $mariadbMcpPath = $null
    $localBin = Join-Path $env:USERPROFILE ".local\bin"
    $mariadbExe = Join-Path $localBin "mcp-server-mariadb.exe"
    if (Test-Path $mariadbExe) {
        $mariadbMcpPath = $mariadbExe
        Write-Host "  Found mcp-server-mariadb at: $mariadbMcpPath" -ForegroundColor Green
    } else {
        $mariadbExeNoExt = Join-Path $localBin "mcp-server-mariadb"
        if (Test-Path $mariadbExeNoExt) {
            $mariadbMcpPath = $mariadbExeNoExt
            Write-Host "  Found mcp-server-mariadb at: $mariadbMcpPath" -ForegroundColor Green
        }
    }
    
    # Merge: Start with template, then add any existing servers not in template
    $mergedServers = @{}
    
    # Add all from template
    foreach ($key in $templateConfig.mcpServers.PSObject.Properties.Name) {
        $mergedServers[$key] = $templateConfig.mcpServers.$key
        
        # Update command for local-maria MCP to use direct executable path
        if ($key -eq "local-maria" -and $mariadbMcpPath) {
            $mergedServers[$key].command = $mariadbMcpPath
            $mergedServers[$key].args = @()  # Remove args since we're using direct executable
            Write-Host "  Updated local-maria MCP to use: $mariadbMcpPath" -ForegroundColor Cyan
        } elseif ($key -eq "local-maria" -and -not $mariadbMcpPath) {
            Write-Host "  Warning: local-maria MCP will not work. Install mcp-server-mariadb first." -ForegroundColor Yellow
        }
    }
    
    # Add existing servers not in template (preserve user's custom MCPs like KIS)
    if ($existingConfig.mcpServers) {
        foreach ($key in $existingConfig.mcpServers.PSObject.Properties.Name) {
            if (-not $mergedServers.ContainsKey($key)) {
                $mergedServers[$key] = $existingConfig.mcpServers.$key
                Write-Host "  Preserved existing MCP: $key" -ForegroundColor Cyan
            }
        }
    }
    
    # Create final config
    $finalConfig = @{
        mcpServers = $mergedServers
    }
    
    # Write to file
    $mcpDir = Split-Path $mcpConfigPath -Parent
    if (-not (Test-Path $mcpDir)) {
        New-Item -ItemType Directory -Path $mcpDir -Force | Out-Null
    }
    
    $jsonContent = $finalConfig | ConvertTo-Json -Depth 10
    [System.IO.File]::WriteAllText($mcpConfigPath, $jsonContent, [System.Text.Encoding]::UTF8)
    
    Write-Host "  [OK] MCP configuration updated: $mcpConfigPath" -ForegroundColor Green
} else {
    Write-Host "  [WARNING] MCP configuration template not found." -ForegroundColor Yellow
}

# Summary
Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  MCP Installation Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

Write-Host "`nInstalled MCP Servers:" -ForegroundColor Yellow
Write-Host "  [OK] Filesystem MCP (npx)" -ForegroundColor Green
Write-Host "  [OK] GitHub MCP (npx)" -ForegroundColor Green
Write-Host "  [OK] Notion MCP (npx)" -ForegroundColor Green

$mariadbMcpInstalled = $false
try {
    $localBin = Join-Path $env:USERPROFILE ".local\bin"
    $mariadbExe = Join-Path $localBin "mcp-server-mariadb.exe"
    if (Test-Path $mariadbExe) {
        $mariadbMcpInstalled = $true
        Write-Host "  [OK] MariaDB MCP (uv tool)" -ForegroundColor Green
    } else {
        Write-Host "  [SKIP] MariaDB MCP (not installed)" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [SKIP] MariaDB MCP (not available)" -ForegroundColor Yellow
}

Write-Host "`nConfiguration:" -ForegroundColor Yellow
Write-Host "  MCP Config: $mcpConfigPath" -ForegroundColor White

Write-Host "`nNext Steps:" -ForegroundColor Cyan
Write-Host "  1. Set environment variables (if not set):" -ForegroundColor White
Write-Host "     GITHUB_TOKEN: [System.Environment]::SetEnvironmentVariable('GITHUB_TOKEN', 'your_token', 'User')" -ForegroundColor Yellow
Write-Host "     NOTION_TOKEN: [System.Environment]::SetEnvironmentVariable('NOTION_TOKEN', 'your_token', 'User')" -ForegroundColor Yellow
Write-Host "  2. Install 한국투자증권 MCP (KIS Code Assistant):" -ForegroundColor White
Write-Host "     Visit: https://smithery.ai/server/@KISOpenAPI/kis-code-assistant-mcp" -ForegroundColor Yellow
Write-Host "     Click 'One-Click Install' and follow the instructions" -ForegroundColor Yellow
Write-Host "  3. Restart Cursor to apply MCP changes" -ForegroundColor White

Write-Host "========================================`n" -ForegroundColor Cyan
