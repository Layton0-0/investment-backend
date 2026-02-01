@echo off
REM Run setup-local-complete.ps1 without requiring PowerShell execution policy change.
REM Usage: .\scripts\setup-local-complete.cmd [options]
REM Options: -SkipPrerequisites -SkipDocker -SkipPython -SkipEnv
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0setup-local-complete.ps1" %*
