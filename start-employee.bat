@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
echo [EMPLOYEE] starting on http://localhost:8083
"%JAVA%" -Dfile.encoding=UTF-8 -jar target\attendance-system-1.0.0.jar --server.port=8083 --app.port-role=EMPLOYEE --app.port-role-label=员工系统
pause
