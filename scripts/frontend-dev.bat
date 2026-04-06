@echo off
chcp 936 >nul 2>&1

setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul 2>&1
if errorlevel 1 goto :bad_root
for %%I in (.) do set "ROOT_DIR=%%~fI"
popd >nul 2>&1

title frontend-dev-5173
cd /d "%ROOT_DIR%\frontend"

echo ========================================
echo  Frontend (local dev)
echo ========================================
echo Root: %ROOT_DIR%
echo.

echo ========================================
echo [INFO] Checking port 5173...
echo ========================================

for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":5173" ^| findstr LISTENING') do (
  echo [WARN] Port 5173 is in use. Killing PID %%P ...
  taskkill /PID %%P /F >nul 2>&1
)

where node >nul 2>&1
if errorlevel 1 goto :node_missing

where npm >nul 2>&1
if errorlevel 1 goto :npm_missing

if not exist "%ROOT_DIR%\frontend\package.json" goto :missing_package_json

if not exist "%ROOT_DIR%\frontend\node_modules" goto :install_deps

echo [INFO] Starting dev server...
call npm run dev
exit /b %errorlevel%

:install_deps
echo [INFO] node_modules not found. Installing deps...
call npm install --legacy-peer-deps
if errorlevel 1 goto :npm_install_failed
goto :start_dev

:start_dev
echo [INFO] Starting dev server...
call npm run dev
exit /b %errorlevel%

:node_missing
echo [ERROR] node not found in PATH.
echo Install Node.js and restart your terminal.
pause
exit /b 1

:npm_missing
echo [ERROR] npm not found in PATH.
echo Install Node.js (includes npm) and restart your terminal.
pause
exit /b 1

:missing_package_json
echo [ERROR] Missing file: %ROOT_DIR%\frontend\package.json
pause
exit /b 1

:npm_install_failed
echo [ERROR] npm install failed.
pause
exit /b 1

:bad_root
echo [ERROR] Failed to resolve project root directory.
echo Script dir: %SCRIPT_DIR%
pause
exit /b 1

