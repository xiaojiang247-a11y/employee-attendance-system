@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting 3 role ports: 8081 SUPER_ADMIN / 8082 ADMIN / 8083 EMPLOYEE
start "SUPER-8081" "%~dp0start-super.bat"
start "ADMIN-8082" "%~dp0start-admin.bat"
start "EMPLOYEE-8083" "%~dp0start-employee.bat"
echo All three instances launched. Wait ~10s then open the URLs above.
