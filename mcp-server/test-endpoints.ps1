# MCP Server Test Script
Write-Host "Testing MCP Server Endpoints..." -ForegroundColor Green
Write-Host ""

# Test 1: Health Check
Write-Host "1. Testing Health Check..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host "✓ Health Status: $($health.status)" -ForegroundColor Green
} catch {
    Write-Host "✗ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Initialize
Write-Host "2. Testing MCP Initialization..." -ForegroundColor Yellow
try {
    $initRequest = @{
        jsonrpc = "2.0"
        id = 1
        method = "initialize"
        params = @{
            protocolVersion = "2024-11-05"
            capabilities = @{
                tools = @{}
            }
            clientInfo = @{
                name = "powershell-test-client"
                version = "1.0.0"
            }
        }
    }
    $initJson = $initRequest | ConvertTo-Json -Depth 4
    $initResponse = Invoke-RestMethod -Uri "http://localhost:8080/mcp/initialize" -Method Post -Body $initJson -ContentType "application/json"
    Write-Host "✓ MCP Initialization successful" -ForegroundColor Green
    Write-Host "  Protocol Version: $($initResponse.result.protocolVersion)" -ForegroundColor Cyan
} catch {
    Write-Host "✗ MCP initialization failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: List Tools
Write-Host "3. Testing Tool List..." -ForegroundColor Yellow
try {
    $toolsRequest = @{
        jsonrpc = "2.0"
        id = 2
        method = "tools/list"
        params = @{}
    }
    $toolsJson = $toolsRequest | ConvertTo-Json -Depth 3
    $toolsResponse = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/list" -Method Post -Body $toolsJson -ContentType "application/json"
    Write-Host "✓ Available tools:" -ForegroundColor Green
    foreach ($tool in $toolsResponse.result.tools) {
        Write-Host "  • $($tool.name): $($tool.description)" -ForegroundColor Cyan
    }
} catch {
    Write-Host "✗ Tool list failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Echo Tool
Write-Host "4. Testing Echo Tool..." -ForegroundColor Yellow
try {
    $echoRequest = @{
        jsonrpc = "2.0"
        id = 3
        method = "tools/call"
        params = @{
            name = "echo"
            arguments = @{
                message = "Hello MCP Server!"
            }
        }
    }
    $echoJson = $echoRequest | ConvertTo-Json -Depth 4
    $echoResponse = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $echoJson -ContentType "application/json"
    Write-Host "✓ Echo response: $($echoResponse.result.content[0].text)" -ForegroundColor Green
} catch {
    Write-Host "✗ Echo tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 5: Current Time Tool
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
    }
    $timeJson = $timeRequest | ConvertTo-Json -Depth 4
    $timeResponse = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $timeJson -ContentType "application/json"
    Write-Host "✓ Current time: $($timeResponse.result.content[0].text)" -ForegroundColor Green
} catch {
    Write-Host "✗ Current time tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 6: Calculate Tool
Write-Host "6. Testing Calculate Tool..." -ForegroundColor Yellow
try {
    $calcRequest = @{
        jsonrpc = "2.0"
        id = 5
        method = "tools/call"
        params = @{
            name = "calculate"
            arguments = @{
                expression = "2 + 2 * 3"
            }
        }
    }
    $calcJson = $calcRequest | ConvertTo-Json -Depth 4
    $calcResponse = Invoke-RestMethod -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $calcJson -ContentType "application/json"
    Write-Host "✓ Calculation result: $($calcResponse.result.content[0].text)" -ForegroundColor Green
} catch {
    Write-Host "✗ Calculate tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "MCP Server testing completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Available endpoints:" -ForegroundColor Cyan
Write-Host "  • REST API: http://localhost:8080/mcp/" -ForegroundColor Cyan
Write-Host "  • SSE Stream: http://localhost:8080/mcp/sse" -ForegroundColor Cyan
Write-Host "  • WebSocket: ws://localhost:8080/mcp/ws" -ForegroundColor Cyan
Write-Host "  • Health Check: http://localhost:8080/actuator/health" -ForegroundColor Cyan
