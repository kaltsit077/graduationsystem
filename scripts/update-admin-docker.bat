@echo off
chcp 65001 >nul 2>&1
setlocal
echo Updating admin password...

powershell -Command "Get-Content backend\src\main\resources\db\update-admin-password.sql | docker exec -i mysql-graduation mysql -u root -proot graduation_topic"

if %errorlevel% equ 0 (
    echo [OK] Password updated.
    echo.
    echo Login:
    echo   username: admin
    echo   password: 123456
) else (
    echo [ERROR] Update failed. Check:
    echo   1. Container running: docker ps ^| findstr mysql
    echo   2. DB password (default: root)
    echo   3. DB name: graduation_topic
    echo.
    echo Or run manually:
    echo   docker exec -it mysql-graduation mysql -u root -proot graduation_topic
    echo   then execute:
    echo   UPDATE user SET password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA' WHERE username = 'admin';
)

pause

