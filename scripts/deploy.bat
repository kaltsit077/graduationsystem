@echo off
chcp 65001 >nul 2>&1
setlocal

echo ========================================
echo  Production deployment
echo ========================================
echo.

if not exist ".env" (
    echo [ERROR] Missing .env file.
    echo.
    echo Create .env and set at least:
    echo   MYSQL_ROOT_PASSWORD=...
    echo   MYSQL_PASSWORD=...
    echo   DB_PASSWORD=...
    echo   JWT_SECRET=...
    echo   ADMIN_PASSWORD=...
    echo.
    echo You can copy from .env.example
    pause
    exit /b 1
)

echo [1/4] Checking Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker not found. Install Docker Desktop first.
    pause
    exit /b 1
)
echo [OK] Docker detected.

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] docker-compose not found.
    pause
    exit /b 1
)
echo [OK] docker-compose detected.
echo.

echo [2/4] Stopping existing services...
docker-compose -f docker-compose.prod.yml down
echo [OK] Stopped.
echo.

echo [3/4] Building images...
docker-compose -f docker-compose.prod.yml build --no-cache
if errorlevel 1 (
    echo [ERROR] Build failed.
    pause
    exit /b 1
)
echo [OK] Build complete.
echo.

echo [4/4] Starting services...
docker-compose -f docker-compose.prod.yml up -d
if errorlevel 1 (
    echo [ERROR] Start failed.
    pause
    exit /b 1
)
echo [OK] Services started.
echo.

echo ========================================
echo  Done
echo ========================================
echo.
echo [Status]
docker-compose -f docker-compose.prod.yml ps
echo.
echo [URLs]
echo   Frontend: http://localhost
echo   Backend API: http://localhost:9090/api
echo   Health: http://localhost:9090/api/ping
echo.
echo [Logs]
echo   docker-compose -f docker-compose.prod.yml logs -f
echo.
echo [Stop]
echo   docker-compose -f docker-compose.prod.yml down
echo.
pause


