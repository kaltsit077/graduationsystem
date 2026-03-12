@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul 2>&1
title Docker dev
color 0B

echo ========================================
echo  Docker dev (foreground)
echo  Live mounts / hot reload
echo ========================================
echo.

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%.."

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] docker not found. Install Docker Desktop first.
    pause
    exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is installed but the daemon is not running.
    echo - Please start Docker Desktop and wait until it shows "Running".
    pause
    exit /b 1
)
echo [OK] Docker detected and daemon is running
echo.

if not exist "docker-compose.dev.yml" (
    echo [ERROR] Missing docker-compose.dev.yml
    pause
    exit /b 1
)

echo ========================================
echo  Starting Docker dev stack
echo ========================================
echo.
echo [INFO] Starting services (MySQL, backend, frontend)...
echo [INFO] First run may take a while (pull images, install deps)
echo [INFO] Code changes should hot reload
echo.

REM Foreground run so you can see logs and Ctrl+C to stop
docker compose -f docker-compose.dev.yml up
if errorlevel 1 (
  echo [WARN] "docker compose" failed, trying "docker-compose"...
  docker-compose -f docker-compose.dev.yml up
)

echo.
echo ========================================
echo  Services stopped
echo ========================================
pause
exit /b 0

