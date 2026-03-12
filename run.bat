@echo off
setlocal
chcp 65001 >nul 2>&1

set "ROOT_DIR=%~dp0"
cd /d "%ROOT_DIR%"

if not exist "scripts\\docker-up.bat" (
  echo [ERROR] Missing script: scripts\\docker-up.bat
  pause
  exit /b 1
)

call "scripts\\docker-up.bat"

