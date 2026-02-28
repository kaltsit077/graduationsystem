@echo off
setlocal enabledelayedexpansion
REM ========================================
REM  毕业论文选题系统 - 一键启动脚本
REM  自动启动后端和前端服务
REM ========================================

chcp 65001 >nul 2>&1
title 毕业论文选题系统 - 启动服务
color 0A

echo ========================================
echo  毕业论文选题系统 - 启动服务
echo ========================================
echo.

REM 脚本在 scripts 目录下，项目根目录为其上一级（含 backend、frontend）
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
cd /d "%PROJECT_ROOT%"

REM ========================================
REM  1. 环境检查
REM ========================================
echo [1/6] 检查运行环境...
echo.

REM 检查Docker（用于MySQL）
where docker >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Docker，请安装Docker Desktop
    echo 下载地址: https://www.docker.com/products/docker-desktop/
    pause
    exit /b 1
)
echo [✓] Docker已安装

REM 检查Docker Desktop是否运行
docker info >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker Desktop 未运行，请先启动 Docker Desktop
    echo.
    echo [提示] 请执行以下步骤：
    echo   1. 打开 Docker Desktop 应用程序
    echo   2. 等待 Docker 完全启动（状态栏图标不再闪烁）
    echo   3. 重新运行此脚本
    echo.
    echo [尝试] 正在尝试自动启动 Docker Desktop...
    start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe" 2>nul
    if errorlevel 1 (
        start "" "%LOCALAPPDATA%\Docker\Docker Desktop.exe" 2>nul
    )
    echo [等待] 等待 Docker Desktop 启动（最多30秒）...
    timeout /t 5 /nobreak >nul
    for /L %%i in (1,1,6) do (
        docker info >nul 2>&1
        if not errorlevel 1 (
            echo [✓] Docker Desktop 已启动
            goto :docker_ready
        )
        timeout /t 5 /nobreak >nul
    )
    echo [警告] Docker Desktop 可能仍在启动中，请稍后重试
    echo [提示] 如果 Docker Desktop 未自动启动，请手动打开它
    pause
    exit /b 1
)
:docker_ready
echo [✓] Docker Desktop 正在运行

REM 检查并启动MySQL容器
echo [检查] MySQL容器状态...
docker ps -a | findstr "mysql-graduation" >nul 2>&1
if errorlevel 1 (
    echo [信息] MySQL容器不存在，正在创建...
    docker-compose up -d mysql
    if errorlevel 1 (
        echo [错误] MySQL容器启动失败
        pause
        exit /b 1
    )
    echo [✓] MySQL容器已创建并启动
) else (
    docker ps | findstr "mysql-graduation" | findstr "Up" >nul 2>&1
    if errorlevel 1 (
        echo [信息] MySQL容器已存在但未运行，正在启动...
        docker start mysql-graduation
        if errorlevel 1 (
            echo [错误] MySQL容器启动失败
            pause
            exit /b 1
        )
        echo [✓] MySQL容器已启动
    ) else (
        echo [✓] MySQL容器正在运行
    )
)
echo [等待] MySQL容器健康检查（最多30秒）...
timeout /t 5 /nobreak >nul
for /L %%i in (1,1,6) do (
    docker exec mysql-graduation mysqladmin ping -h localhost -uroot -proot >nul 2>&1
    if not errorlevel 1 (
        echo [✓] MySQL容器已就绪
        goto :mysql_ready
    )
    timeout /t 5 /nobreak >nul
)
echo [警告] MySQL容器可能未完全就绪，但将继续启动...
:mysql_ready

REM 检查Java
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Java环境，请安装JDK 17或更高版本
    echo 下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo [✓] Java环境检查通过

REM 检查Node.js
where node >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Node.js，请安装Node.js 16或更高版本
    echo 下载地址: https://nodejs.org/
    pause
    exit /b 1
)
echo [✓] Node.js环境检查通过

REM 检查npm
where npm >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到npm
    pause
    exit /b 1
)
echo [✓] npm环境检查通过
echo.

REM ========================================
REM  2. 目录检查
REM ========================================
echo [2/6] 检查项目目录...
echo.

if not exist "backend" (
    echo [错误] 未找到backend目录
    pause
    exit /b 1
)
echo [✓] backend目录存在

if not exist "frontend" (
    echo [错误] 未找到frontend目录
    pause
    exit /b 1
)
echo [✓] frontend目录存在
echo.

REM ========================================
REM  3. 端口检查和强制关闭
REM ========================================
echo [3/6] 检查并释放端口...
echo.

REM 检查并关闭9090端口（后端）
echo [检查] 端口9090...
netstat -ano | findstr ":9090" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 端口9090已被占用，正在强制关闭...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":9090" ^| findstr "LISTENING"') do (
        set "PID=%%a"
        echo [关闭] 进程PID: %%a
        taskkill /PID %%a /F >nul 2>&1
        if errorlevel 1 (
            taskkill /PID %%a /F /T >nul 2>&1
        )
    )
    timeout /t 2 /nobreak >nul
    echo [✓] 端口9090已释放
) else (
    echo [✓] 端口9090可用
)

REM 检查并关闭5173端口（前端）
echo [检查] 端口5173...
netstat -ano | findstr ":5173" | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo [警告] 端口5173已被占用，正在强制关闭...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
        set "PID=%%a"
        echo [关闭] 进程PID: %%a
        taskkill /PID %%a /F >nul 2>&1
        if errorlevel 1 (
            taskkill /PID %%a /F /T >nul 2>&1
        )
    )
    timeout /t 2 /nobreak >nul
    echo [✓] 端口5173已释放
) else (
    echo [✓] 端口5173可用
)
echo.

REM ========================================
REM  4. 启动后端服务
REM ========================================
echo [4/6] 启动后端服务...
echo.

cd /d "%PROJECT_ROOT%\backend"

REM 检查是否有Maven
where mvn >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到Maven，无法编译项目
    echo [提示] 请安装Maven或手动运行: cd backend ^&^& mvn clean package
    pause
    exit /b 1
)

REM 强制重新编译以确保使用最新配置
echo [信息] 正在重新编译项目以使用新端口配置...
start "后端服务-9090" cmd /k "chcp 65001 >nul 2>&1 && cd /d "%PROJECT_ROOT%\backend" && title 后端服务 - 端口9090 && echo ======================================== && echo  后端服务启动中... && echo ======================================== && echo. && echo [信息] 正在清理并重新打包项目，请稍候... && call mvn clean package -DskipTests -q && echo [信息] 打包完成，启动服务（端口9090）... && java -jar -Dspring.profiles.active=prod target\graduation-topic-backend-0.0.1-SNAPSHOT.jar"
echo [✓] 后端服务已在新窗口启动（正在编译中...）
echo.

REM ========================================
REM  5. 启动前端服务
REM ========================================
echo [5/6] 启动前端服务...
echo.

cd /d "%PROJECT_ROOT%\frontend"

REM 检查node_modules是否存在
if not exist "node_modules" (
    echo [信息] 未找到node_modules，正在安装依赖...
    npm config set registry https://registry.npmmirror.com >nul 2>&1
    call npm install --legacy-peer-deps
    if errorlevel 1 (
        echo [错误] 依赖安装失败
        pause
        exit /b 1
    )
    echo [✓] 依赖安装完成
)

echo [信息] 启动前端开发服务器...
start "前端服务-5173" cmd /k "chcp 65001 >nul 2>&1 && cd /d "%PROJECT_ROOT%\frontend" && title 前端服务 - 端口5173 && echo ======================================== && echo  前端服务启动中... && echo ======================================== && echo. && npm run dev"
echo [✓] 前端服务已在新窗口启动
echo.

REM ========================================
REM  启动完成
REM ========================================
timeout /t 3 /nobreak >nul
echo ========================================
echo  服务启动完成！
echo ========================================
echo.
echo [访问地址]
echo   前端: http://localhost:5173
echo   后端API: http://localhost:9090/api
echo   健康检查: http://localhost:9090/api/ping
echo.
echo [服务状态]
echo   MySQL: Docker容器（mysql-graduation）
echo   后端: 本地运行（后端服务-9090窗口）
echo   前端: 本地运行（前端服务-5173窗口）
echo.
echo [停止服务]
echo   关闭对应的服务窗口，或按Ctrl+C
echo   MySQL容器: docker stop mysql-graduation
echo.
echo [提示] 后端服务可能需要30-60秒才能完全启动
echo [提示] 如果前端无法连接后端，请等待后端启动完成
echo [提示] 如果数据库连接失败，请检查MySQL容器是否正常运行
echo.

REM 询问是否打开浏览器
choice /C YN /M "是否打开浏览器访问前端页面"
if errorlevel 2 goto :end
if errorlevel 1 (
    timeout /t 2 /nobreak >nul
    start http://localhost:5173
)

:end
echo.
echo ========================================
echo  按任意键退出（服务将继续运行）...
echo ========================================
pause >nul

exit /b 0

