@echo off
chcp 65001 >nul
echo ========================================
echo    WMS Quick Start
echo ========================================
echo.
echo Please select:
echo [1] Start Server Only
echo [2] Start Client Only
echo [3] Start Both Server and Client
echo [4] Exit
echo.

set /p choice="Enter your choice (1-4): "

if "%choice%"=="1" goto server
if "%choice%"=="2" goto client
if "%choice%"=="3" goto both
if "%choice%"=="4" goto end

echo Invalid choice!
pause
goto end

:server
echo.
echo Starting server...
start "WMS Server" cmd /k call start_server.bat
echo Server started in new window
pause
goto end

:client
echo.
echo Starting client...
echo Please make sure server is running!
echo.
pause
call mvnw.cmd javafx:run
goto end

:both
echo.
echo Starting server...
start "WMS Server" cmd /k call start_server.bat
echo Waiting 5 seconds for server to start...
timeout /t 5 /nobreak >nul
echo.
echo Starting client...
call mvnw.cmd javafx:run
goto end

:end
exit
