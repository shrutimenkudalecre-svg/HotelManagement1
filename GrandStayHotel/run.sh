#!/bin/bash
echo "========================================"
echo " Grand Stay Hotel - Build & Run Script"
echo "========================================"

mkdir -p backend/lib backend/out

echo ""
echo "[1/2] Compiling Java source files..."
javac -cp "backend/lib/*" -d backend/out backend/src/hotel/*.java

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: Compilation failed!"
    echo "Make sure you have:"
    echo "  - JDK installed"
    echo "  - mysql-connector-j-*.jar in backend/lib/"
    echo "  - json-20231013.jar in backend/lib/"
    exit 1
fi

echo "Compilation successful!"
echo ""
echo "[2/2] Starting Hotel Server on port 8080..."
echo "Press Ctrl+C to stop."
echo ""
java -cp "backend/out:backend/lib/*" hotel.HotelServer
