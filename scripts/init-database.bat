@echo off
chcp 65001 >nul 2>&1
setlocal
echo ========================================
echo  Initialize database
echo ========================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] docker not found. Install Docker Desktop first.
    pause
    exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is installed but the daemon is not running.
    echo Start Docker Desktop ^(Linux containers^) first, then re-run this script.
    pause
    exit /b 1
)

set "MYSQL_CONTAINER="
docker inspect mysql-graduation-dev >nul 2>&1
if not errorlevel 1 set "MYSQL_CONTAINER=mysql-graduation-dev"
if not defined MYSQL_CONTAINER (
  docker inspect mysql-graduation >nul 2>&1
  if not errorlevel 1 set "MYSQL_CONTAINER=mysql-graduation"
)
if not defined MYSQL_CONTAINER (
    echo [ERROR] MySQL container is not running.
    echo - If you use dev compose, run: docker-compose -f docker-compose.dev.yml up -d
    echo - If you use prod compose, run: docker-compose up -d
    pause
    exit /b 1
)
echo [OK] Using MySQL container: %MYSQL_CONTAINER%

echo [1/3] Create database (if missing)...
docker exec %MYSQL_CONTAINER% mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS graduation_topic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Failed to create database
    pause
    exit /b 1
)
echo [OK] Database ready

echo.
echo [2/3] Apply schema.sql...
docker cp backend\src\main\resources\db\schema.sql %MYSQL_CONTAINER%:/tmp/schema.sql >nul 2>&1
docker exec -i %MYSQL_CONTAINER% sh -c "mysql -u root -proot graduation_topic < /tmp/schema.sql" >nul 2>&1
if errorlevel 1 (
    echo [WARN] Schema may already exist or failed
) else (
    echo [OK] Schema applied
)

echo.
if exist "backend\src\main\resources\db\init-data.sql" (
    echo [3/3] Apply init-data.sql...
    docker cp backend\src\main\resources\db\init-data.sql %MYSQL_CONTAINER%:/tmp/init-data.sql >nul 2>&1
    docker exec -i %MYSQL_CONTAINER% sh -c "mysql -u root -proot graduation_topic < /tmp/init-data.sql" 2>nul
    if errorlevel 1 (
        echo [WARN] Seed data may already exist or failed
    ) else (
        echo [OK] Seed data applied
    )
) else (
    echo [3/3] init-data.sql not found, skipping seed data.
)

echo.
echo ========================================
echo  Done
echo ========================================
echo.
echo [Default accounts]
echo   admin: admin / 123456
echo   teacher: teacher001 / 123456
echo   student: student001 / 123456
echo.
echo [Verification]
docker exec %MYSQL_CONTAINER% mysql -u root -proot -e "USE graduation_topic; SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema='graduation_topic';" 2>nul
docker exec %MYSQL_CONTAINER% mysql -u root -proot -e "USE graduation_topic; SHOW TABLES LIKE 'topic_metrics';" 2>nul
docker exec %MYSQL_CONTAINER% mysql -u root -proot -e "USE graduation_topic; SELECT username, role FROM user;" 2>nul
echo.
pause

