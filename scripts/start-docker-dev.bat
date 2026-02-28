@echo off
setlocal enabledelayedexpansion
REM ========================================
REM  毕业论文选题系统 - Docker 开发模式启动脚本
REM  支持代码挂载和热更新，无需重新构建镜像
REM ========================================

chcp 65001 >nul 2>&1
title 毕业论文选题系统 - Docker 开发模式
color 0B

echo ========================================
echo  毕业论文选题系统 - Docker 开发模式
echo  支持代码挂载和热更新
echo ========================================
echo.

REM 获取脚本所在目录
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

REM 检查 Docker
where docker >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Docker，请安装Docker Desktop
    echo 下载地址: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)
echo [✓] Docker环境检查通过
echo.

REM 检查 docker-compose.dev.yml
if not exist "docker-compose.dev.yml" (
    echo [错误] 未找到 docker-compose.dev.yml 文件
    pause
    exit /b 1
)

echo ========================================
echo  启动 Docker 开发环境
echo ========================================
echo.
echo [信息] 正在启动服务（MySQL、后端、前端）...
echo [提示] 首次启动可能需要几分钟来下载镜像和安装依赖
echo [提示] 代码修改后会自动热更新（无需重新构建）
echo.

REM 启动服务
docker-compose -f docker-compose.dev.yml up

echo.
echo ========================================
echo  服务已停止
echo ========================================
pause

exit /b 0

