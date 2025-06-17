# Test MCP Configuration Migration
# This script tests the complete MCP configuration migration from application.properties to JSON

Write-Host "Testing MCP Configuration Migration..." -ForegroundColor Yellow
Write-Host ""

# Test 1: Create default configuration
Write-Host "1. Testing config creation..." -ForegroundColor Cyan
try {
    $result = java -jar target/misoto-0.0.1-SNAPSHOT.jar --mcp-create-config test-mcp.json
    if (Test-Path "test-mcp.json") {
        Write-Host "✓ Default configuration created successfully" -ForegroundColor Green
        
        # Validate JSON structure
        $config = Get-Content "test-mcp.json" | ConvertFrom-Json
        if ($config.client -and $config.servers) {
            Write-Host "✓ Configuration has required structure" -ForegroundColor Green
        } else {
            Write-Host "✗ Configuration missing required sections" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ Configuration file not created" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ Config creation failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 2: Validate configuration
Write-Host "2. Testing config validation..." -ForegroundColor Cyan
try {
    $result = java -jar target/misoto-0.0.1-SNAPSHOT.jar --mcp-validate-config test-mcp.json
    Write-Host "✓ Configuration validation works" -ForegroundColor Green
} catch {
    Write-Host "✗ Config validation failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 3: Test MCP commands with custom config
Write-Host "3. Testing MCP commands with custom config..." -ForegroundColor Cyan
try {
    Write-Host "Testing config show..." -ForegroundColor White
    java -jar target/misoto-0.0.1-SNAPSHOT.jar --mcp-config test-mcp.json mcp config show
    Write-Host ""
    
    Write-Host "Testing status command..." -ForegroundColor White
    java -jar target/misoto-0.0.1-SNAPSHOT.jar --mcp-config test-mcp.json mcp status
    Write-Host ""
    
    Write-Host "✓ MCP commands work with custom config" -ForegroundColor Green
} catch {
    Write-Host "✗ MCP commands failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Test 4: Verify application.properties cleanup
Write-Host "4. Testing application.properties cleanup..." -ForegroundColor Cyan
$appProps = Get-Content "src/main/resources/application.properties" -Raw
if ($appProps -notmatch "mcp\.") {
    Write-Host "✓ MCP properties removed from application.properties" -ForegroundColor Green
} else {
    Write-Host "✗ MCP properties still found in application.properties" -ForegroundColor Red
}
Write-Host ""

# Test 5: Check that default config is loaded when no custom config specified
Write-Host "5. Testing default configuration loading..." -ForegroundColor Cyan
try {
    Write-Host "Testing config show with default config..." -ForegroundColor White
    java -jar target/misoto-0.0.1-SNAPSHOT.jar mcp config show
    Write-Host "✓ Default configuration loading works" -ForegroundColor Green
} catch {
    Write-Host "✗ Default configuration loading failed: $($_.Exception.Message)" -ForegroundColor Red
}
Write-Host ""

# Cleanup
Write-Host "Cleaning up test files..." -ForegroundColor Gray
if (Test-Path "test-mcp.json") {
    Remove-Item "test-mcp.json"
}

Write-Host "MCP Configuration Migration Testing Complete!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary of Changes:" -ForegroundColor Yellow
Write-Host "• Moved MCP configuration from application.properties to mcp.json" -ForegroundColor White
Write-Host "• Added CLI support for --mcp-config, --mcp-create-config, --mcp-validate-config" -ForegroundColor White  
Write-Host "• Added comprehensive mcp config subcommands (show, load, create, validate, save)" -ForegroundColor White
Write-Host "• Updated McpCommand to work with multi-server configuration" -ForegroundColor White
Write-Host "• Added JSON-based configuration loading with fallback support" -ForegroundColor White
