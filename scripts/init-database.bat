@echo off
REM ========================================
REM  初始化数据库脚本
REM  执行 schema.sql 和 init-data.sql
REM ========================================

chcp 65001 >nul 2>&1
echo ========================================
echo  初始化数据库
echo ========================================
echo.

REM 检查 Docker 容器是否运行
docker ps | findstr mysql-graduation >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] MySQL 容器未运行，请先启动：
    echo   docker-compose up -d
    pause
    exit /b 1
)

echo [1/3] 创建数据库（如果不存在）...
docker exec mysql-graduation mysql -u root -proot -e "CREATE DATABASE IF NOT EXISTS graduation_topic DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
if %errorlevel% equ 0 (
    echo [✓] 数据库已就绪
) else (
    echo [错误] 数据库创建失败
    pause
    exit /b 1
)

echo.
echo [2/3] 执行建表脚本（schema.sql）...
docker cp backend\src\main\resources\db\schema.sql mysql-graduation:/tmp/schema.sql >nul 2>&1
docker exec -i mysql-graduation sh -c "mysql -u root -proot graduation_topic < /tmp/schema.sql" >nul 2>&1
if %errorlevel% equ 0 (
    echo [✓] 数据表创建成功
) else (
    echo [警告] 建表脚本执行可能有问题（表可能已存在）
)

echo.
echo [3/3] 执行初始化数据脚本（init-data.sql）...
docker cp backend\src\main\resources\db\init-data.sql mysql-graduation:/tmp/init-data.sql >nul 2>&1
docker exec -i mysql-graduation sh -c "mysql -u root -proot graduation_topic < /tmp/init-data.sql" 2>nul
if %errorlevel% equ 0 (
    echo [✓] 初始化数据导入成功
) else (
    echo [警告] 部分数据可能已存在（这是正常的）
)

echo.
echo ========================================
echo  数据库初始化完成！
echo ========================================
echo.
echo [默认账号信息]
echo   管理员: admin / 123456
echo   导师: teacher001 / 123456
echo   学生: student001 / 123456
echo.
echo [验证]
docker exec mysql-graduation mysql -u root -proot -e "USE graduation_topic; SELECT COUNT(*) as table_count FROM information_schema.tables WHERE table_schema='graduation_topic';" 2>nul
docker exec mysql-graduation mysql -u root -proot -e "USE graduation_topic; SELECT username, role FROM user;" 2>nul
echo.
pause

