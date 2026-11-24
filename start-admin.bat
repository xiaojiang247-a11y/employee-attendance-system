@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "JAVA=java"
if defined JAVA_HOME set "JAVA=%JAVA_HOME%\bin\java.exe"
echo [ADMIN] starting on http://localhost:8082
"%JAVA%" -Dfile.encoding=UTF-8 -jar target\attendance-system-1.0.0.jar --server.port=8082 --app.port-role=ADMIN --app.port-role-label=管理员系统
pause
