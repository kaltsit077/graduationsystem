@echo off
chcp 65001 >nul 2>&1
if /I "%DEBUG%"=="1" echo on

setlocal EnableExtensions
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "ROOT_DIR=%%~fI"

title backend-local-9090
cd /d "%ROOT_DIR%\backend"

echo ========================================
echo  Backend (local jar)
echo ========================================
echo Root: %ROOT_DIR%
echo.

where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] java not found in PATH.
  echo Install JDK and restart your terminal.
  pause
  exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
  echo [ERROR] mvn not found in PATH.
  echo Install Maven and restart your terminal.
  pause
  exit /b 1
)

echo [INFO] Checking port 9090...
set "PORT=9090"

REM Safer behavior: only kill Java processes (avoid killing docker-proxy / compose).
REM To force kill whatever occupies the port: set FORCE_KILL=1 before running this script.
set "FOUND_LISTENER="
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":%PORT%" ^| findstr "LISTENING"') do (
  set "FOUND_LISTENER=1"
  call :HandlePortListener %%P
  if errorlevel 1 exit /b 1
)

if not defined FOUND_LISTENER (
  echo [INFO] Port %PORT% is free.
) else (
  echo [INFO] Port %PORT% should now be free (or was free already).
)
echo.

goto :AfterPortCheck

:HandlePortListener
set "PID=%~1"
set "PROC_NAME="
for /f "tokens=1 delims=," %%N in ('tasklist /FI "PID eq %PID%" /FO CSV /NH') do set "PROC_NAME=%%~N"

if /I "%FORCE_KILL%"=="1" (
  echo [WARN] FORCE_KILL=1 set. Killing PID %PID% [%PROC_NAME%] on port %PORT%...
  taskkill /PID %PID% /F /T >nul 2>&1
  exit /b 0
)

if /I "%PROC_NAME%"=="java.exe" (
  echo [INFO] Killing Java PID %PID% on port %PORT%...
  taskkill /PID %PID% /F /T >nul 2>&1
  exit /b 0
)

if /I "%PROC_NAME%"=="javaw.exe" (
  echo [INFO] Killing Java PID %PID% on port %PORT%...
  taskkill /PID %PID% /F /T >nul 2>&1
  exit /b 0
)

echo [ERROR] Port %PORT% is in use by PID %PID% [%PROC_NAME%].
echo [ERROR] Not killing it to avoid stopping Docker/other services.
echo [HINT] If this is Docker Compose, stop it first, or use a different port.
echo [HINT] To force kill anyway: set FORCE_KILL=1 ^&^& "%~f0"
pause
exit /b 1

:AfterPortCheck

set "JAR_NAME=graduation-topic-backend-0.0.1-SNAPSHOT.jar"
set "JAR_PATH=%ROOT_DIR%\backend\target\%JAR_NAME%"

if not exist "%JAR_PATH%" (
  echo [INFO] Jar not found. Building...
  call mvn -DskipTests clean package
  if errorlevel 1 (
    echo [ERROR] Maven build failed.
    pause
    exit /b 1
  )
)

if not exist "%JAR_PATH%" (
  echo [ERROR] Jar still not found: %JAR_PATH%
  pause
  exit /b 1
)

echo [INFO] Starting jar...
java -Dspring.profiles.active=prod -jar "%JAR_PATH%"

echo.
echo [INFO] Ensure MySQL is running (for example docker container "mysql-graduation" on localhost:3306).

