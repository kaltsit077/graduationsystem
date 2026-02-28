@echo off
REM ========================================
REM  生产环境部署脚本
REM ========================================

chcp 65001 >nul 2>&1
echo ========================================
echo  生产环境部署脚本
echo ========================================
echo.

REM 检查 .env 文件
if not exist ".env" (
    echo [错误] 未找到 .env 文件
    echo.
    echo 请先创建 .env 文件并配置以下内容：
    echo   MYSQL_ROOT_PASSWORD=你的强密码
    echo   MYSQL_PASSWORD=你的强密码
    echo   DB_PASSWORD=你的强密码
    echo   JWT_SECRET=你的64字节密钥
    echo   ADMIN_PASSWORD=你的强密码
    echo.
    echo 可以参考 .env.example 文件
    pause
    exit /b 1
)

echo [1/4] 检查 Docker 环境...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未安装 Docker，请先安装 Docker Desktop
    pause
    exit /b 1
)
echo [✓] Docker 环境检查通过

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未安装 Docker Compose
    pause
    exit /b 1
)
echo [✓] Docker Compose 环境检查通过
echo.

echo [2/4] 停止现有服务...
docker-compose -f docker-compose.prod.yml down
echo [✓] 服务已停止
echo.

echo [3/4] 构建镜像...
docker-compose -f docker-compose.prod.yml build --no-cache
if errorlevel 1 (
    echo [错误] 镜像构建失败
    pause
    exit /b 1
)
echo [✓] 镜像构建完成
echo.

echo [4/4] 启动服务...
docker-compose -f docker-compose.prod.yml up -d
if errorlevel 1 (
    echo [错误] 服务启动失败
    pause
    exit /b 1
)
echo [✓] 服务启动成功
echo.

echo ========================================
echo  部署完成！
echo ========================================
echo.
echo [服务状态]
docker-compose -f docker-compose.prod.yml ps
echo.
echo [访问地址]
echo   前端: http://localhost
echo   后端API: http://localhost:9090/api
echo   健康检查: http://localhost:9090/api/ping
echo.
echo [查看日志]
echo   docker-compose -f docker-compose.prod.yml logs -f
echo.
echo [停止服务]
echo   docker-compose -f docker-compose.prod.yml down
echo.
pause


