# 스트레스 구간 백필 + 팩터 계산 + 백테스트 실행. 결과를 JSON으로 출력하여 backtest-stress-results.md §3 기입에 사용.
# 사용: .\scripts\run-stress-backtest.ps1 -BaseUrl "http://localhost:8080" [-Username "user"] [-Password "pass"] [-EnvPath "..\.env"]
# Username/Password 미지정 시 EnvPath에서 SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD 읽음.

param(
    [string] $BaseUrl = "http://localhost:8080",
    [string] $Username = "",
    [string] $Password = "",
    [string] $EnvPath = "",
    [string] $OutJsonPath = ""
)

$ErrorActionPreference = "Stop"
$api = $BaseUrl.TrimEnd("/")

# .env from SUPER_ADMIN_USERNAME, SUPER_ADMIN_PASSWORD
if ((-not $Username) -or (-not $Password)) {
    $envFile = $EnvPath
    if (-not $envFile) {
        $tryPath = Join-Path $PSScriptRoot "..\.env"
        if (Test-Path $tryPath) { $envFile = (Resolve-Path $tryPath).Path }
    }
    if (-not $envFile -and (Test-Path ".\.env")) { $envFile = (Resolve-Path ".\.env").Path }
    if ($envFile -and (Test-Path $envFile)) {
        Get-Content $envFile -Encoding UTF8 | ForEach-Object {
            $line = $_.Trim()
            if ($line -match '^\s*SUPER_ADMIN_USERNAME=(.+)$') { $script:Username = $Matches[1].Trim().Trim('"') }
            if ($line -match '^\s*SUPER_ADMIN_PASSWORD=(.+)$') { $script:Password = $Matches[1].Trim().Trim('"') }
        }
    }
    if (-not $script:Username) { $script:Username = $env:STRESS_TEST_USER }
    if (-not $script:Password) { $script:Password = $env:STRESS_TEST_PASSWORD }
}

if (-not $Username -or -not $Password) {
    Write-Host "Username/Password 필요. -Username, -Password 또는 -EnvPath(SUPER_ADMIN_*), 또는 STRESS_TEST_USER/STRESS_TEST_PASSWORD 환경변수." -ForegroundColor Red
    exit 1
}

function Invoke-Api {
    param([string]$Method, [string]$Path, [hashtable]$Headers = @{}, [object]$Body = $null)
    $url = "$api$Path"
    $h = @{ "Content-Type" = "application/json" }
    foreach ($k in $Headers.Keys) { $h[$k] = $Headers[$k] }
    $params = @{ Uri = $url; Method = $Method; Headers = $h; UseBasicParsing = $true }
    if ($Body) { $params.Body = ($Body | ConvertTo-Json -Compress) }
    try {
        $r = Invoke-WebRequest @params -TimeoutSec 120
        return @{ StatusCode = $r.StatusCode; Content = $r.Content }
    } catch {
        $status = 0
        $errBody = $_.Exception.Message
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $stream = $_.Exception.Response.GetResponseStream()
            if ($stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $errBody = $reader.ReadToEnd()
                $reader.Close()
            }
        }
        return @{ StatusCode = $status; Content = $errBody; Error = $_.Exception.Message }
    }
}

# 1) 로그인
Write-Host "Login..." -ForegroundColor Cyan
$loginRes = Invoke-Api -Method POST -Path "/api/v1/auth/login" -Body @{ username = $Username; password = $Password }
if ($loginRes.StatusCode -ne 200) {
    Write-Host "Login failed: $($loginRes.StatusCode) $($loginRes.Content)" -ForegroundColor Red
    exit 1
}
$auth = $loginRes.Content | ConvertFrom-Json
$token = $auth.token
if (-not $token) {
    Write-Host "Login response has no token." -ForegroundColor Red
    exit 1
}
$headers = @{ "Authorization" = "Bearer $token" }
Write-Host "Login OK." -ForegroundColor Green

# 2) Backfill (COVID then rate-hike)
$backfills = @(
    @{ name = "KR COVID"; path = "/api/v1/trigger/krx-daily-backfill"; from = "2020-02-01"; to = "2020-04-30" },
    @{ name = "US COVID"; path = "/api/v1/trigger/us-daily-backfill"; from = "2020-02-01"; to = "2020-04-30" },
    @{ name = "KR Rate"; path = "/api/v1/trigger/krx-daily-backfill"; from = "2022-01-01"; to = "2022-06-30" },
    @{ name = "US Rate"; path = "/api/v1/trigger/us-daily-backfill"; from = "2022-01-01"; to = "2022-06-30" }
)
foreach ($b in $backfills) {
    $q = "?from=" + $b.from + "&to=" + $b.to
    $path = $b.path + $q
    Write-Host "Backfill $($b.name)..." -ForegroundColor Cyan
    $r = Invoke-Api -Method POST -Path $path -Headers $headers
    $len = [Math]::Min(200, $r.Content.Length)
    Write-Host "  -> $($r.StatusCode) $($r.Content.Substring(0, $len))"
}

# 3) 팩터 계산
Write-Host "Factor calculation..." -ForegroundColor Cyan
$r = Invoke-Api -Method POST -Path "/api/v1/trigger/factor-calculation" -Headers $headers
Write-Host "  -> $($r.StatusCode)"

# 4) Backtest x4 (COVID KR/US, Rate KR/US)
$runs = @(
    @{ scenario = "COVID"; start = "2020-02-24"; end = "2020-04-30"; market = "KR"; strategy = "SHORT_TERM" },
    @{ scenario = "COVID"; start = "2020-02-24"; end = "2020-04-30"; market = "US"; strategy = "SHORT_TERM" },
    @{ scenario = "Rate"; start = "2022-01-03"; end = "2022-06-30"; market = "KR"; strategy = "SHORT_TERM" },
    @{ scenario = "Rate"; start = "2022-01-03"; end = "2022-06-30"; market = "US"; strategy = "SHORT_TERM" }
)
$results = @()
foreach ($run in $runs) {
    $body = @{
        startDate     = $run.start
        endDate       = $run.end
        market        = $run.market
        strategyType  = $run.strategy
        initialCapital = 100000000
    }
    Write-Host "Backtest $($run.scenario) $($run.market) $($run.strategy)..." -ForegroundColor Cyan
    $r = Invoke-Api -Method POST -Path "/api/v1/backtest" -Headers $headers -Body $body
    $entry = @{
        scenario     = $run.scenario
        startDate    = $run.start
        endDate      = $run.end
        market       = $run.market
        strategyType = $run.strategy
        statusCode   = $r.StatusCode
        raw          = $r.Content
    }
    if ($r.StatusCode -eq 200) {
        $parsed = $r.Content | ConvertFrom-Json
        $entry.mddPct = $parsed.mddPct
        $entry.cagr = $parsed.cagr
        $entry.tradeCount = $parsed.tradeCount
        $entry.trades = $parsed.trades
        $entry.finalCapital = $parsed.finalCapital
        Write-Host "  -> MDD=$($entry.mddPct) CAGR=$($entry.cagr) trades=$($entry.tradeCount)"
    } else {
        Write-Host "  -> $($r.StatusCode) $($r.Content.Substring(0, [Math]::Min(300, $r.Content.Length)))"
    }
    $results += $entry
}

$report = @{
    runAt    = (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
    baseUrl  = $BaseUrl
    backtestResults = $results
}
$json = $report | ConvertTo-Json -Depth 10
if ($OutJsonPath) {
    $OutJsonPath | Set-Content -Value $json -Encoding UTF8
    Write-Host "Results written to $OutJsonPath" -ForegroundColor Green
} else {
    $outPath = Join-Path $PSScriptRoot "..\docs\02-architecture\stress-backtest-results.json"
    $outPath = (Resolve-Path $outPath -ErrorAction SilentlyContinue).Path
    if (-not $outPath) { $outPath = "stress-backtest-results.json" }
    $json | Set-Content -Path $outPath -Encoding UTF8
    Write-Host "Results written to $outPath" -ForegroundColor Green
}
Write-Host "Done." -ForegroundColor Green
