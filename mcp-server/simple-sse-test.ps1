# Simple SSE Connection Test
Write-Host "Testing SSE Connection..." 

try {
    # Test SSE connection
    $job = Start-Job -ScriptBlock {
        $response = Invoke-WebRequest -Uri "http://localhost:8080/mcp/stream?clientId=test-client" -Method Get -Headers @{"Accept"="text/event-stream"; "Cache-Control"="no-cache"} -TimeoutSec 5
        return $response.StatusCode
    }
    
    $result = Wait-Job $job -Timeout 5 | Receive-Job
    Remove-Job $job
    
    if ($result -eq 200) {
        Write-Host "SSE endpoint is working - Status: $result" -ForegroundColor Green
    } else {
        Write-Host "SSE endpoint returned status: $result" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "SSE test failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test sending a message to the SSE client
Write-Host "Testing SSE Message Send..."
try {
    $message = @{
        event = "test"
        data = @{
            message = "Hello via SSE!"
            timestamp = (Get-Date).ToString()
        }
    } | ConvertTo-Json -Depth 3
    
    $response = Invoke-WebRequest -Uri "http://localhost:8080/mcp/stream/test-client/send" -Method Post -Body $message -ContentType "application/json"
    $result = $response.Content | ConvertFrom-Json
    
    Write-Host "Message sent successfully - Status: $($result.status)" -ForegroundColor Green
    
} catch {
    Write-Host "Message send failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "SSE testing completed."
