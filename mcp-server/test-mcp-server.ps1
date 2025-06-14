# Test MCP Server Endpoints
# PowerShell script to test the MCP server functionality

Write-Host "Testing MCP Server Endpoints..." -ForegroundColor Green
Write-Host ""

# Test 1: Health Check
Write-Host "1. Testing Health Check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host "✓ Health Status: $($health.status)" -ForegroundColor Green
    Write-Host "  Components: $($health.components.Keys -join ', ')" -ForegroundColor Cyan
} catch {
    Write-Host "✗ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: List Tools
Write-Host "2. Testing Tool List..." -ForegroundColor Yellow
try {
    $toolsRequest = @{
        jsonrpc = "2.0"
        id = 1
        method = "tools/list"
        params = @{}
    } | ConvertTo-Json -Depth 3

    $tools = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/list" -Method Post -Body $toolsRequest -ContentType "application/json"
    Write-Host "✓ Available tools:" -ForegroundColor Green
    foreach ($tool in $tools.result.tools) {
        Write-Host "  - $($tool.name): $($tool.description)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "✗ Tools list failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Execute Echo Tool
Write-Host "3. Testing Echo Tool..." -ForegroundColor Yellow
try {
    $echoRequest = @{
        jsonrpc = "2.0"
        id = 2
        method = "tools/call"
        params = @{
            name = "echo"
            arguments = @{
                text = "Hello from MCP Server!"
            }
        }
    } | ConvertTo-Json -Depth 4

    $echoResult = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $echoRequest -ContentType "application/json"
    Write-Host "✓ Echo result: $($echoResult.result.content[0].text)" -ForegroundColor Green
} catch {
    Write-Host "✗ Echo tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Execute Calculate Tool
Write-Host "4. Testing Calculate Tool..." -ForegroundColor Yellow
try {
    $calcRequest = @{
        jsonrpc = "2.0"
        id = 3
        method = "tools/call"
        params = @{
            name = "calculate"
            arguments = @{
                expression = "2 + 2 * 3"
            }
        }
    } | ConvertTo-Json -Depth 4

    $calcResult = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $calcRequest -ContentType "application/json"
    Write-Host "✓ Calculation result: $($calcResult.result.content[0].text)" -ForegroundColor Green
} catch {
    Write-Host "✗ Calculate tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 5: Get Current Time
Write-Host "5. Testing Current Time Tool..." -ForegroundColor Yellow
try {
    $timeRequest = @{
        jsonrpc = "2.0"
        id = 4
        method = "tools/call"
        params = @{
            name = "current_time"
            arguments = @{}
        }
    } | ConvertTo-Json -Depth 3

    $timeResult = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $timeRequest -ContentType "application/json"
    Write-Host "✓ Current time: $($timeResult.result.content[0].text)" -ForegroundColor Green
} catch {
    Write-Host "✗ Current time tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 6: Initialize MCP Connection
Write-Host "6. Testing MCP Initialization..." -ForegroundColor Yellow
try {
    $initRequest = @{
        jsonrpc = "2.0"
        id = 5
        method = "initialize"
        params = @{
            protocolVersion = "2024-11-05"
            capabilities = @{
                tools = @{}
                resources = @{}
            }
            clientInfo = @{
                name = "powershell-test-client"
                version = "1.0.0"
            }
        }
    } | ConvertTo-Json -Depth 4

    $initResult = Invoke-RestMethod -Uri "http://localhost:8080/mcp/initialize" -Method Post -Body $initRequest -ContentType "application/json"
    Write-Host "✓ MCP Initialization successful" -ForegroundColor Green
    Write-Host "  Protocol Version: $($initResult.result.protocolVersion)" -ForegroundColor Cyan
    Write-Host "  Server Info: $($initResult.result.serverInfo.name) v$($initResult.result.serverInfo.version)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ MCP initialization failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "MCP Server testing completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Available endpoints:" -ForegroundColor Yellow
Write-Host "  • REST API: http://localhost:8080/mcp/" -ForegroundColor Cyan
Write-Host "  • SSE Stream: http://localhost:8080/mcp/sse" -ForegroundColor Cyan
Write-Host "  • WebSocket: ws://localhost:8080/mcp/ws" -ForegroundColor Cyan
Write-Host "  • Health Check: http://localhost:8080/actuator/health" -ForegroundColor Cyan
