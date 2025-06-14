# SSE Test Script
Write-Host "Testing SSE Endpoint..." -ForegroundColor Green

try {    # Create a web request to the SSE endpoint
    $request = [System.Net.WebRequest]::Create("http://localhost:8080/mcp/stream?clientId=test-client")
    $request.Method = "GET"
    $request.ContentType = "text/event-stream"
    
    # Get the response stream
    $response = $request.GetResponse()
    $stream = $response.GetResponseStream()
    $reader = New-Object System.IO.StreamReader($stream)
    
    Write-Host "✓ Connected to SSE endpoint" -ForegroundColor Green
    Write-Host "Receiving events (will timeout after 10 seconds):" -ForegroundColor Yellow
    
    $timeout = (Get-Date).AddSeconds(10)
    $eventCount = 0
    
    while ((Get-Date) -lt $timeout) {
        if ($reader.Peek() -ge 0) {
            $line = $reader.ReadLine()
            if ($line -ne $null -and $line.Trim() -ne "") {
                Write-Host "  Event: $line" -ForegroundColor Cyan
                $eventCount++
            }
        }
        Start-Sleep -Milliseconds 100
    }
    
    Write-Host "✓ Received $eventCount events from SSE endpoint" -ForegroundColor Green
    
    $reader.Close()
    $stream.Close()
    $response.Close()
    
} catch {
    Write-Host "✗ SSE test failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "SSE testing completed." -ForegroundColor Green
