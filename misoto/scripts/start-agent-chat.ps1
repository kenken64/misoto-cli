#!/usr/bin/env pwsh
# PowerShell script to start Misoto chat with agent mode enabled

Write-Host "🤖 Starting Misoto Chat with Agent Mode Enabled..." -ForegroundColor Cyan

# Set environment variable to enable agent mode
$env:MISOTO_AGENT_MODE = "true"

# Change to the root directory (one level up from scripts)
$rootDir = Split-Path -Parent $PSScriptRoot
Set-Location $rootDir

# Check if the JAR file exists
$jarFile = "target/misoto-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $jarFile)) {
    Write-Host "❌ JAR file not found: $jarFile" -ForegroundColor Red
    Write-Host "   Please build the project first with: mvn clean package -DskipTests" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Agent mode enabled (MISOTO_AGENT_MODE=true)" -ForegroundColor Green
Write-Host "🚀 Starting interactive chat..." -ForegroundColor Green
Write-Host ""

# Start the application
java -jar $jarFile chat
