@echo off
echo Fixing user permissions...

REM Try common MySQL paths
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set MYSQL_PATH="C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) else if exist "C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe" (
    set MYSQL_PATH="C:\Program Files\MySQL\MySQL Server 5.7\bin\mysql.exe"
) else if exist "C:\xampp\mysql\bin\mysql.exe" (
    set MYSQL_PATH="C:\xampp\mysql\bin\mysql.exe"
) else if exist "C:\wamp64\bin\mysql\mysql8.0.31\bin\mysql.exe" (
    set MYSQL_PATH="C:\wamp64\bin\mysql\mysql8.0.31\bin\mysql.exe"
) else (
    echo MySQL executable not found in common paths. Please update this script with your MySQL path.
    pause
    exit /b 1
)

echo Using MySQL at: %MYSQL_PATH%
%MYSQL_PATH% -u root -p765614 event_management_db < fix-user-permissions.sql

if %ERRORLEVEL% EQU 0 (
    echo Permissions fixed successfully!
) else (
    echo Error fixing permissions. Please check the error message above.
)

pause