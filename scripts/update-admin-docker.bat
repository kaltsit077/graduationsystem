@echo off
REM ========================================
REM  使用Docker更新管理员密码
REM ========================================

chcp 65001 >nul 2>&1
echo 正在更新管理员密码...

REM 方法1: 使用PowerShell管道
powershell -Command "Get-Content backend\src\main\resources\db\update-admin-password.sql | docker exec -i mysql-graduation mysql -u root -proot graduation_topic"

if %errorlevel% equ 0 (
    echo [✓] 密码更新成功！
    echo.
    echo 现在可以使用以下账号登录：
    echo   用户名: admin
    echo   密码: 123456
) else (
    echo [错误] 密码更新失败，请检查：
    echo   1. Docker容器是否运行: docker ps | findstr mysql
    echo   2. 数据库密码是否正确（默认: root）
    echo   3. 数据库名称是否正确: graduation_topic
    echo.
    echo 或者手动执行SQL：
    echo   docker exec -it mysql-graduation mysql -u root -proot graduation_topic
    echo   然后执行: UPDATE user SET password_hash = '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iwy9iYhA' WHERE username = 'admin';
)

pause

