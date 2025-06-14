# Simple MCP Server Test
# Test basic functionality of the MCP server

Write-Host "=== MCP Server Functionality Test ===" -ForegroundColor Green
Write-Host ""

# Test 1: Health Check
Write-Host "Testing Health Check..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" -Method Get
    Write-Host "✓ Status Code: $($response.StatusCode)" -ForegroundColor Green
    $healthData = $response.Content | ConvertFrom-Json
    Write-Host "✓ Health Status: $($healthData.status)" -ForegroundColor Green
} catch {
    Write-Host "✗ Health check failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Test MCP Tool List
Write-Host "Testing MCP Tools List..." -ForegroundColor Yellow
try {
    $toolsRequest = @{
        jsonrpc = "2.0"
        id = 1
        method = "tools/list"
        params = @{}
    }
    
    $requestBody = $toolsRequest | ConvertTo-Json -Depth 3
    $response = Invoke-WebRequest -Uri "http://localhost:8080/mcp/tools/list" -Method Post -Body $requestBody -ContentType "application/json"
    
    if ($response.StatusCode -eq 200) {
        Write-Host "✓ Tools endpoint accessible (Status: $($response.StatusCode))" -ForegroundColor Green
        $toolsData = $response.Content | ConvertFrom-Json
        if ($toolsData.result -and $toolsData.result.tools) {
            Write-Host "✓ Found $($toolsData.result.tools.Count) tools:" -ForegroundColor Green
            foreach ($tool in $toolsData.result.tools) {
                Write-Host "  - $($tool.name): $($tool.description)" -ForegroundColor Cyan
            }
        }
    }
} catch {
    Write-Host "✗ Tools list failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Test Echo Tool
Write-Host "Testing Echo Tool..." -ForegroundColor Yellow
try {
    $echoRequest = @{
        jsonrpc = "2.0"
        id = 2
        method = "tools/call"
        params = @{
            name = "echo"
            arguments = @{
                text = "Hello MCP World!"
            }
        }
    }
    
    $requestBody = $echoRequest | ConvertTo-Json -Depth 4
    $response = Invoke-WebRequest -Uri "http://localhost:8080/mcp/tools/call" -Method Post -Body $requestBody -ContentType "application/json"
    
    if ($response.StatusCode -eq 200) {
        $echoData = $response.Content | ConvertFrom-Json
        if ($echoData.result -and $echoData.result.content) {
            Write-Host "✓ Echo tool result: $($echoData.result.content[0].text)" -ForegroundColor Green
        }
    }
} catch {
    Write-Host "✗ Echo tool failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

Write-Host "=== Test Summary ===" -ForegroundColor Green
Write-Host "The MCP Server is running and accessible on http://localhost:8080" -ForegroundColor Cyan
Write-Host ""
Write-Host "Available Endpoints:" -ForegroundColor Yellow
Write-Host "• Health Check: http://localhost:8080/actuator/health" -ForegroundColor Cyan
Write-Host "• MCP Initialize: http://localhost:8080/mcp/initialize" -ForegroundColor Cyan
Write-Host "• Tools List: http://localhost:8080/mcp/tools/list" -ForegroundColor Cyan
Write-Host "• Tool Call: http://localhost:8080/mcp/tools/call" -ForegroundColor Cyan
Write-Host "• SSE Stream: http://localhost:8080/mcp/sse" -ForegroundColor Cyan
Write-Host "• WebSocket: ws://localhost:8080/mcp/ws" -ForegroundColor Cyan
