@echo off
chcp 65001 >nul 2>&1

setlocal EnableExtensions
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "ROOT_DIR=%%~fI"

echo ========================================
echo  Dev All: embedding + backend + frontend
echo ========================================
echo Root: %ROOT_DIR%
echo.

REM Start embedding service (port 8000)
start "embedding-dev-8000" cmd /k call "%ROOT_DIR%\scripts\embedding-dev.bat"

REM Start backend (port 9090)
start "backend-local-9090" cmd /k call "%ROOT_DIR%\scripts\backend-local.bat"

REM Start frontend (port 5173)
start "frontend-dev-5173" cmd /k call "%ROOT_DIR%\scripts\frontend-dev.bat"

echo [INFO] Launched 3 terminals.
echo [INFO] Frontend: http://localhost:5173
echo [INFO] Backend:   http://localhost:9090
echo [INFO] Embed:     http://localhost:8000
echo.
exit /b 0

