@echo off
REM Batch script to start Misoto chat with agent mode enabled

echo 🤖 Starting Misoto Chat with Agent Mode Enabled...

REM Set environment variable to enable agent mode
set MISOTO_AGENT_MODE=true

REM Change to the root directory (one level up from scripts)
cd /d "%~dp0.."

REM Check if the JAR file exists
if not exist "target\misoto-0.0.1-SNAPSHOT.jar" (
    echo ❌ JAR file not found: target\misoto-0.0.1-SNAPSHOT.jar
    echo    Please build the project first with: mvn clean package -DskipTests
    pause
    exit /b 1
)

echo ✅ Agent mode enabled (MISOTO_AGENT_MODE=true)
echo 🚀 Starting interactive chat...
echo.

REM Start the application
java -jar target\misoto-0.0.1-SNAPSHOT.jar chat

pause
