param(
  [string]$TaskName = "Cursor-GlobalStorage-Migrate",
  [int]$WaitSeconds = 3600
)

$ErrorActionPreference = "Stop"

function Write-Log([string]$message) {
  $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
  $line = "[$ts] $message"
  Write-Output $line
  try {
    $logDir = "D:\works\etc\cursor"
    New-Item -ItemType Directory -Force -Path $logDir | Out-Null
    Add-Content -LiteralPath (Join-Path $logDir "migrate-globalStorage.log") -Value $line -Encoding UTF8
  } catch {
    # ignore log failures
  }
}

function Get-FreeSpaceLine() {
  try {
    $c = Get-PSDrive -Name C -PSProvider FileSystem
    $d = Get-PSDrive -Name D -PSProvider FileSystem
    return ("C: free={0:N2}GB, D: free={1:N2}GB" -f ($c.Free/1GB), ($d.Free/1GB))
  } catch {
    return "free space: unavailable"
  }
}

function Acquire-Lock([string]$lockPath) {
  try {
    New-Item -ItemType Directory -Force -Path (Split-Path $lockPath -Parent) | Out-Null
    # keep the stream open to hold an exclusive lock
    return [System.IO.File]::Open($lockPath, [System.IO.FileMode]::OpenOrCreate, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
  } catch {
    return $null
  }
}

function Wait-UntilCursorExit([int]$seconds) {
  $deadline = (Get-Date).AddSeconds($seconds)
  while ((Get-Date) -lt $deadline) {
    $p = Get-Process -Name Cursor -ErrorAction SilentlyContinue
    if (-not $p) { return $true }
    Start-Sleep -Seconds 5
  }
  return $false
}

Write-Log "Start migration. $([string](Get-FreeSpaceLine))"

$lockPath = "D:\works\etc\cursor\migrate-globalStorage.lock"
$lock = Acquire-Lock -lockPath $lockPath
if (-not $lock) {
  Write-Log "Another migration instance is running (lock=$lockPath). Exiting."
  exit 0
}

try {
  Write-Log "Waiting for Cursor.exe exit (timeout=${WaitSeconds}s)..."

  if (-not (Wait-UntilCursorExit -seconds $WaitSeconds)) {
    Write-Log "Timeout waiting for Cursor exit. Aborting."
    exit 2
  }

  Write-Log "Cursor.exe not running. Proceeding with globalStorage migration."

  $src = Join-Path $env:APPDATA "Cursor\User\globalStorage"
  $dst = "D:\works\etc\cursor\globalStorage"
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $bak = Join-Path (Split-Path $src -Parent) ("globalStorage._bak_$stamp")

  if (-not (Test-Path -LiteralPath $src)) {
    Write-Log "Source not found: $src (nothing to do)"
    exit 0
  }

  $srcItem = Get-Item -LiteralPath $src -ErrorAction Stop
  if ($srcItem.LinkType) {
    Write-Log "Source already a link ($($srcItem.LinkType)) -> $($srcItem.Target). Nothing to do."
    exit 0
  }

  New-Item -ItemType Directory -Force -Path (Split-Path $dst -Parent) | Out-Null
  New-Item -ItemType Directory -Force -Path $dst | Out-Null

  Write-Log "Robocopy COPY src -> dst (no delete): $src -> $dst"
  & robocopy $src $dst /E /COPY:DAT /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null

  Write-Log "Rename source to backup: $bak"
  Rename-Item -LiteralPath $src -NewName (Split-Path $bak -Leaf) -ErrorAction Stop

  Write-Log "Create junction: $src -> $dst"
  New-Item -ItemType Junction -Path $src -Target $dst | Out-Null

  Write-Log "Robocopy MOVE remaining bak -> dst: $bak -> $dst"
  & robocopy $bak $dst /E /MOVE /COPY:DAT /R:1 /W:1 /NFL /NDL /NJH /NJS | Out-Null

  try {
    Remove-Item -LiteralPath $bak -Recurse -Force -ErrorAction SilentlyContinue
  } catch {
    # ignore
  }

  $link = Get-Item -LiteralPath $src -ErrorAction Stop
  Write-Log "Done. LinkType=$($link.LinkType), Target=$($link.Target)"
  Write-Log "End migration. $([string](Get-FreeSpaceLine))"

  try {
    # self-clean: remove scheduled task if exists
    & schtasks /Query /TN $TaskName | Out-Null
    & schtasks /Delete /TN $TaskName /F | Out-Null
    Write-Log "Deleted scheduled task: $TaskName"
  } catch {
    # ignore
  }
} finally {
  try { $lock.Dispose() } catch {}
  try { Remove-Item -LiteralPath $lockPath -Force -ErrorAction SilentlyContinue } catch {}
}

