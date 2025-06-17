# PowerShell script to build and run Misoto application
Write-Host "Building Misoto Application..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build successful! Starting application..." -ForegroundColor Green
    java -jar target\misoto-0.0.1-SNAPSHOT.jar
} else {
    Write-Host "Build failed!" -ForegroundColor Red
    Read-Host "Press Enter to continue"
}
