@echo off
echo ========================================
echo  Grand Stay Hotel - Build & Run Script
echo ========================================

if not exist "backend\lib" mkdir backend\lib
if not exist "backend\out" mkdir backend\out

echo.
echo [1/2] Compiling Java source files...
javac -cp "backend\lib\*" -d backend\out backend\src\hotel\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    echo Make sure you have:
    echo  - JDK installed
    echo  - mysql-connector-j-*.jar in backend\lib\
    echo  - json-20231013.jar in backend\lib\
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo [2/2] Starting Hotel Server on port 8080...
echo Press Ctrl+C to stop the server.
echo.
java -cp "backend\out;backend\lib\*" hotel.HotelServer
pause
