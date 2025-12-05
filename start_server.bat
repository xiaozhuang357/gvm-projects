@echo off
chcp 65001 >nul
echo ========================================
echo    WMS Server Startup
echo ========================================
echo.
echo Starting server...
echo.

cd /d "%~dp0"

rem Compile using Maven
echo Compiling project with Maven...
call mvnw.cmd clean compile -q

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting WMS Server...
echo Port: 8888
echo.
echo Press Ctrl+C to stop server
echo.

rem Start server
call mvnw.cmd exec:java -Dexec.mainClass="com._404.wms.network.WMSServer" -q

pause
