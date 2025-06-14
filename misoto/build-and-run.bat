@echo off
echo Building Misoto Application...
mvn clean package -DskipTests
if %ERRORLEVEL% equ 0 (
    echo Build successful! Starting application...
    java -jar target\misoto-0.0.1-SNAPSHOT.jar
) else (
    echo Build failed!
    pause
)
