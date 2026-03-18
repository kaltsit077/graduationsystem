@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul 2>&1
title Docker dev - rebuild backend (no data loss)
color 0B

echo ========================================
echo  Docker dev 快速部署脚本
echo  功能：重建并启动本地开发用的 backend
echo  特点：
echo    - 只构建 backend 镜像
echo    - 不删除 volume，不会清空 MySQL 数据
echo    - MySQL / 前端容器若已在运行会复用
echo ========================================
echo.

REM 切到项目根目录
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%.."

REM 检查 Docker
where docker >nul 2>&1
if errorlevel 1 (
    echo [ERROR] 未找到 docker 命令，请先安装 Docker Desktop。
    pause
    exit /b 1
)
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 已安装但未启动，请先启动 Docker Desktop。
    pause
    exit /b 1
)
echo [OK] Docker 正常运行。
echo.

REM 检查开发 compose 文件
if not exist "docker-compose.dev.yml" (
    echo [ERROR] 未找到 docker-compose.dev.yml（请在项目根目录运行本脚本）。
    pause
    exit /b 1
)

echo ========================================
echo  Step 1: 构建 backend 镜像（含最新代码）
echo ========================================
echo.
echo [INFO] 正在构建 backend 镜像（不会动数据库 volume）...
docker compose -f docker-compose.dev.yml build backend
if errorlevel 1 (
    echo [WARN] "docker compose build" 失败，尝试使用 "docker-compose build"...
    docker-compose -f docker-compose.dev.yml build backend
    if errorlevel 1 (
        echo [ERROR] backend 镜像构建失败，请检查上方报错。
        pause
        exit /b 1
    )
)
echo.

echo ========================================
echo  Step 2: 启动 / 重启 dev 容器
echo ========================================
echo.
echo [INFO] 启动（或重启）下列服务：
echo        - MySQL  : mysql-graduation-dev
echo        - Backend: graduation-backend-dev (9090)
echo        - Frontend: graduation-frontend-dev (5173)
echo [INFO] 如容器已存在，将复用 volume，不会清除数据库数据。
echo.

docker compose -f docker-compose.dev.yml up -d mysql backend frontend
if errorlevel 1 (
    echo [WARN] "docker compose up" 失败，尝试使用 "docker-compose up"...
    docker-compose -f docker-compose.dev.yml up -d mysql backend frontend
    if errorlevel 1 (
        echo [ERROR] 启动 dev 容器失败，请检查上方报错。
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo  完成：backend 已用最新代码重建并启动
echo  注意：数据库数据保存在 mysql_data_dev 卷中，不会因为本脚本而丢失。
echo.
echo  常用后续命令：
echo    - 查看 backend 日志：
echo        docker compose -f docker-compose.dev.yml logs -f backend
echo    - 停止 dev 容器（不删数据）：
echo        docker compose -f docker-compose.dev.yml stop
echo ========================================
echo.
pause
exit /b 0

