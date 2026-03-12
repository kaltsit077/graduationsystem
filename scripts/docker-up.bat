@echo off
setlocal EnableExtensions
chcp 65001 >nul 2>&1

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "ROOT_DIR=%%~fI"

echo ========================================
echo  Docker Compose Up (detached)
echo ========================================
echo Root: %ROOT_DIR%
echo.
echo [INFO] This will start services via Docker Compose:
echo - mysql (3306)
echo - backend (9090)
echo - frontend (80)
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] docker not found in PATH.
  echo Install Docker Desktop and restart your terminal.
  pause
  exit /b 1
)

cd /d "%ROOT_DIR%"

if not defined COMPOSE_FILE set "COMPOSE_FILE=docker-compose.yml"

if not exist "%ROOT_DIR%\\%COMPOSE_FILE%" (
  echo [ERROR] Missing compose file: %ROOT_DIR%\\%COMPOSE_FILE%
  echo [HINT] Set COMPOSE_FILE to a different file, e.g.:
  echo [HINT]   set COMPOSE_FILE=docker-compose.dev.yml
  pause
  exit /b 1
)

echo [INFO] Starting Docker Compose...
docker compose -f "%COMPOSE_FILE%" up -d
if errorlevel 1 (
  echo [WARN] "docker compose" failed, trying "docker-compose"...
  docker-compose -f "%COMPOSE_FILE%" up -d
  if errorlevel 1 (
    echo [ERROR] Docker Compose failed.
    pause
    exit /b 1
  )
)

echo.
echo [INFO] Started.
echo - Frontend: http://localhost
echo - Backend:  http://localhost:9090
echo.
pause

