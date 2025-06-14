@echo off
REM Claude Code CLI - Windows Batch Script

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0

REM Check if Maven wrapper exists
if not exist "%SCRIPT_DIR%mvnw.cmd" (
    echo Error: Maven wrapper not found. Please ensure you're running this from the project root.
    exit /b 1
)

REM Build the project if target directory doesn't exist or is empty
if not exist "%SCRIPT_DIR%target\misoto-0.0.1-SNAPSHOT.jar" (
    echo Building project...
    call "%SCRIPT_DIR%mvnw.cmd" clean compile -q
    if errorlevel 1 (
        echo Build failed!
        exit /b 1
    )
)

REM Run the CLI with all passed arguments
java -cp "%SCRIPT_DIR%target\classes;%USERPROFILE%\.m2\repository\*" sg.edu.nus.iss.misoto.MisotoApplication %*
