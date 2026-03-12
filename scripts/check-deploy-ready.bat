@echo off
chcp 65001 >nul 2>&1
setlocal
echo ========================================
echo  Pre-deploy checks
echo ========================================
echo.

set ERROR_COUNT=0

echo [Check 1] .env file...
if exist ".env" (
    echo [OK] .env exists
) else (
    echo [ERROR] .env missing
    echo         Copy .env.example to .env and edit it
    set /a ERROR_COUNT+=1
)

echo.
echo [Check 2] Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker not found
    set /a ERROR_COUNT+=1
) else (
    echo [OK] Docker detected
    docker --version
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] docker-compose not found
    set /a ERROR_COUNT+=1
) else (
    echo [OK] docker-compose detected
    docker-compose --version
)

echo.
echo [Check 3] Required files...
if exist "docker-compose.prod.yml" (
    echo [OK] docker-compose.prod.yml
) else (
    echo [ERROR] Missing docker-compose.prod.yml
    set /a ERROR_COUNT+=1
)

if exist "backend\Dockerfile" (
    echo [OK] backend\Dockerfile
) else (
    echo [ERROR] Missing backend\Dockerfile
    set /a ERROR_COUNT+=1
)

if exist "frontend\Dockerfile" (
    echo [OK] frontend\Dockerfile
) else (
    echo [ERROR] Missing frontend\Dockerfile
    set /a ERROR_COUNT+=1
)

echo.
echo [Check 4] Ports...
netstat -ano | findstr ":80 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [WARN] Port 80 is in use
) else (
    echo [OK] Port 80 is free
)

netstat -ano | findstr ":9090 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [WARN] Port 9090 is in use
) else (
    echo [OK] Port 9090 is free
)

echo.
echo [Check 5] CORS config...
findstr /C:"addAllowedOriginPattern" backend\src\main\java\com\example\graduation\config\CorsConfig.java >nul 2>&1
if %errorlevel% equ 0 (
    echo [WARN] CORS still uses wildcard "*"
    echo        Consider restricting origins in production
) else (
    echo [OK] CORS looks restricted
)

echo.
echo ========================================
if %ERROR_COUNT% equ 0 (
    echo  All checks passed.
    echo ========================================
    echo.
    echo Next:
    echo   1. Ensure .env is configured
    echo   2. Run deploy.bat
) else (
    echo  Found %ERROR_COUNT% issue(s).
    echo ========================================
    echo.
    echo Fix the issues above before deploying.
)
echo.
pause

