@echo off
REM ========================================
REM  Database Quick Reference
REM  Quick database operation reference
REM ========================================

chcp 65001 >nul 2>&1
echo ========================================
echo  Database Quick Reference
echo ========================================
echo.
echo Database Information:
echo   Database: graduation_topic
echo   Username: root
echo   Password: root
echo   Host: localhost:3306
echo.
echo ========================================
echo  Common Commands
echo ========================================
echo.
echo 1. Connect to database:
echo    mysql -u root -p
echo.
echo 2. Switch to target database:
echo    USE graduation_topic;
echo.
echo 3. Show all tables:
echo    SHOW TABLES;
echo.
echo 4. View user table:
echo    SELECT id, username, real_name, role, status FROM user;
echo.
echo 5. View topic table:
echo    SELECT * FROM topic;
echo.
echo 6. View application table:
echo    SELECT * FROM topic_application;
echo.
echo 7. Execute common query script:
echo    mysql -u root -p graduation_topic ^< src\main\resources\db\query-common.sql
echo.
echo ========================================
echo  Documentation
echo ========================================
echo.
echo Full operation guide: DATABASE_GUIDE.md
echo.
pause

