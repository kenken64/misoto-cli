# Test MCP Integration in Misoto CLI
# PowerShell script to test the MCP client functionality

Write-Host "Testing MCP Integration in Misoto CLI" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Green
Write-Host ""

# Check if MCP server is running
Write-Host "1. Checking MCP Server Status..." -ForegroundColor Yellow
try {
    $serverStatus = Invoke-RestMethod -Uri "http://localhost:8080/mcp/health" -Method Get -TimeoutSec 5
    Write-Host "✓ MCP Server is running (Status: $($serverStatus.status))" -ForegroundColor Green
} catch {
    Write-Host "✗ MCP Server is not running or not accessible" -ForegroundColor Red
    Write-Host "Please start the MCP server first." -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Build the misoto project
Write-Host "2. Building Misoto CLI..." -ForegroundColor Yellow
$buildResult = & mvn -q compile
if ($LASTEXITCODE -eq 0) {
    Write-Host "✓ Misoto CLI built successfully" -ForegroundColor Green
} else {
    Write-Host "✗ Failed to build Misoto CLI" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Test MCP commands
Write-Host "3. Testing MCP Commands..." -ForegroundColor Yellow

Write-Host ""
Write-Host "3.1 Testing MCP Status Command..." -ForegroundColor Cyan
& java -cp "target/classes;target/dependency/*" sg.edu.nus.iss.misoto.MisotoApplication mcp status

Write-Host ""
Write-Host "3.2 Testing MCP Initialize Command..." -ForegroundColor Cyan
& java -cp "target/classes;target/dependency/*" sg.edu.nus.iss.misoto.MisotoApplication mcp init

Write-Host ""
Write-Host "3.3 Testing MCP Ping Command..." -ForegroundColor Cyan
& java -cp "target/classes;target/dependency/*" sg.edu.nus.iss.misoto.MisotoApplication mcp ping

Write-Host ""
Write-Host "3.4 Testing MCP Tools List Command..." -ForegroundColor Cyan
& java -cp "target/classes;target/dependency/*" sg.edu.nus.iss.misoto.MisotoApplication mcp tools list

Write-Host ""
Write-Host "3.5 Testing MCP Tool Call (echo)..." -ForegroundColor Cyan
& java -cp "target/classes;target/dependency/*" sg.edu.nus.iss.misoto.MisotoApplication mcp call echo

Write-Host ""
Write-Host "Testing completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Available MCP Commands in Misoto CLI:" -ForegroundColor Yellow
Write-Host "  • mcp status     - Show MCP client status" -ForegroundColor White
Write-Host "  • mcp init       - Initialize MCP connection" -ForegroundColor White
Write-Host "  • mcp ping       - Test server connectivity" -ForegroundColor White
Write-Host "  • mcp tools list - List available tools" -ForegroundColor White
Write-Host "  • mcp call <tool> - Execute a tool" -ForegroundColor White
Write-Host "  • mcp sse        - Connect to SSE stream" -ForegroundColor White
Write-Host "  • mcp websocket  - Connect to WebSocket" -ForegroundColor White
Write-Host ""
Write-Host "Example usage:" -ForegroundColor Yellow
Write-Host "  java -jar target/misoto-0.0.1-SNAPSHOT.jar mcp init" -ForegroundColor Cyan
Write-Host "  java -jar target/misoto-0.0.1-SNAPSHOT.jar mcp tools list" -ForegroundColor Cyan
Write-Host "  java -jar target/misoto-0.0.1-SNAPSHOT.jar mcp call echo" -ForegroundColor Cyan
