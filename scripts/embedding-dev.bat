@echo off
chcp 65001 >nul 2>&1
if /I "%DEBUG%"=="1" echo on

setlocal EnableExtensions
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "ROOT_DIR=%%~fI"

title embedding-dev-8000
cd /d "%ROOT_DIR%\ai-embedding"

echo ========================================
echo  Embedding Service (FastAPI)
echo ========================================
echo Root: %ROOT_DIR%
echo.

where python >nul 2>&1
if errorlevel 1 (
  echo [ERROR] python not found in PATH.
  echo Install Python 3.10+ and restart your terminal.
  pause
  exit /b 1
)

echo [INFO] Checking port 8000...
set "PORT=8000"
set "FOUND_LISTENER="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
  set "FOUND_LISTENER=1"
  echo [WARN] Port %PORT% is in use by PID %%P. Please stop it first.
  pause
  exit /b 1
)
if not defined FOUND_LISTENER (
  echo [INFO] Port %PORT% is free.
)
echo.

set "VENV_DIR=%ROOT_DIR%\ai-embedding\venv"
if exist "%VENV_DIR%\Scripts\python.exe" (
  echo [INFO] Using existing venv: %VENV_DIR%
) else (
  echo [INFO] venv not found. Creating: %VENV_DIR%
  python -m venv "%VENV_DIR%"
  if errorlevel 1 (
    echo [ERROR] Failed to create venv.
    pause
    exit /b 1
  )
)

call "%VENV_DIR%\Scripts\activate.bat"
if errorlevel 1 (
  echo [ERROR] Failed to activate venv.
  pause
  exit /b 1
)

echo [INFO] Ensuring dependencies...
python -m pip install -U pip >nul 2>&1
python -m pip install -U fastapi uvicorn "pydantic<3" FlagEmbedding >nul 2>&1

echo [INFO] Starting embedding server on http://localhost:8000
echo [INFO] Health check: http://localhost:8000/
echo.
python -m uvicorn main:app --host 0.0.0.0 --port 8000

