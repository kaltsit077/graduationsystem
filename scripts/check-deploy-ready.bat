@echo off
REM ========================================
REM  部署前检查脚本
REM  检查部署所需的所有配置是否就绪
REM ========================================

chcp 65001 >nul 2>&1
echo ========================================
echo  部署前检查
echo ========================================
echo.

set ERROR_COUNT=0

REM 检查 .env 文件
echo [检查 1] 环境变量文件...
if exist ".env" (
    echo [✓] .env 文件已存在
) else (
    echo [✗] .env 文件不存在
    echo     请复制 .env.example 并修改为 .env
    set /a ERROR_COUNT+=1
)

REM 检查 Docker
echo.
echo [检查 2] Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [✗] Docker 未安装
    set /a ERROR_COUNT+=1
) else (
    echo [✓] Docker 已安装
    docker --version
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [✗] Docker Compose 未安装
    set /a ERROR_COUNT+=1
) else (
    echo [✓] Docker Compose 已安装
    docker-compose --version
)

REM 检查必要文件
echo.
echo [检查 3] 必要文件...
if exist "docker-compose.prod.yml" (
    echo [✓] docker-compose.prod.yml 存在
) else (
    echo [✗] docker-compose.prod.yml 不存在
    set /a ERROR_COUNT+=1
)

if exist "backend\Dockerfile" (
    echo [✓] backend\Dockerfile 存在
) else (
    echo [✗] backend\Dockerfile 不存在
    set /a ERROR_COUNT+=1
)

if exist "frontend\Dockerfile" (
    echo [✓] frontend\Dockerfile 存在
) else (
    echo [✗] frontend\Dockerfile 不存在
    set /a ERROR_COUNT+=1
)

REM 检查端口占用
echo.
echo [检查 4] 端口占用情况...
netstat -ano | findstr ":80 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 端口 80 已被占用
) else (
    echo [✓] 端口 80 可用
)

netstat -ano | findstr ":9090 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 端口 9090 已被占用
) else (
    echo [✓] 端口 9090 可用
)

REM 检查 CORS 配置
echo.
echo [检查 5] CORS 配置...
findstr /C:"addAllowedOriginPattern" backend\src\main\java\com\example\graduation\config\CorsConfig.java >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] CORS 配置仍使用通配符 "*"
    echo         生产环境建议修改为具体域名
) else (
    echo [✓] CORS 配置已修改
)

echo.
echo ========================================
if %ERROR_COUNT% equ 0 (
    echo  检查完成：所有必要项都已就绪！
    echo ========================================
    echo.
    echo 下一步：
    echo   1. 如果还没有 .env 文件，请创建并配置
    echo   2. 运行 deploy.bat 开始部署
) else (
    echo  检查完成：发现 %ERROR_COUNT% 个问题
    echo ========================================
    echo.
    echo 请先解决上述问题后再继续部署
)
echo.
pause

