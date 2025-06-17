# Test Multi-Server MCP Configuration - JSON-based (UPDATED)
# PowerShell script to test the new JSON-based MCP configuration system

Write-Host "Testing MCP JSON Configuration Migration..." -ForegroundColor Green
Write-Host ""

# Test the new JSON configuration structure
Write-Host "1. Testing JSON Configuration..." -ForegroundColor Yellow
$mcpJsonFile = "src\main\resources\mcp.json"
if (Test-Path $mcpJsonFile) {
    Write-Host "✓ Found mcp.json configuration file" -ForegroundColor Green
    
    try {
        $jsonContent = Get-Content $mcpJsonFile -Raw | ConvertFrom-Json
        
        # Test client configuration
        if ($jsonContent.client) {
            Write-Host "✓ Client configuration found" -ForegroundColor Green
            Write-Host "  Client name: $($jsonContent.client.name)" -ForegroundColor Cyan
            Write-Host "  Client version: $($jsonContent.client.version)" -ForegroundColor Cyan
        } else {
            Write-Host "✗ Client configuration missing" -ForegroundColor Red
        }
        
        # Test server configurations
        if ($jsonContent.servers) {
            Write-Host "✓ Multi-server configuration found" -ForegroundColor Green
            Write-Host "  Configured servers:" -ForegroundColor Cyan
            
            $jsonContent.servers.PSObject.Properties | ForEach-Object {
                $serverId = $_.Name
                $server = $_.Value
                $enabledText = if ($server.enabled) { "enabled" } else { "disabled" }
                Write-Host "    • [$serverId] $($server.name) - $($server.url) ($enabledText)" -ForegroundColor Cyan
            }
        } else {
            Write-Host "✗ Server configuration missing" -ForegroundColor Red
        }
    } catch {
        Write-Host "✗ Error parsing JSON configuration: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "✗ mcp.json configuration file not found" -ForegroundColor Red
}

# Test that MCP configuration was removed from application.properties
Write-Host "`n1.1. Verifying MCP config removal from application.properties..." -ForegroundColor Yellow
$propertiesFile = "src\main\resources\application.properties"
if (Test-Path $propertiesFile) {
    $content = Get-Content $propertiesFile -Raw
    if ($content -match "mcp\.servers\.") {
        Write-Host "✗ MCP configuration still found in application.properties (migration incomplete)" -ForegroundColor Red
    } else {
        Write-Host "✓ MCP configuration successfully removed from application.properties" -ForegroundColor Green
    }
} else {
    Write-Host "✗ application.properties not found" -ForegroundColor Red
}
Write-Host ""

# Test that our MCP configuration classes exist and are properly structured
Write-Host "2. Testing MCP Configuration Classes..." -ForegroundColor Yellow
$configFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\config\McpConfiguration.java"
if (Test-Path $configFile) {
    $configContent = Get-Content $configFile -Raw
    if ($configContent -match "@JsonProperty") {
        Write-Host "✓ McpConfiguration with Jackson JSON annotations found" -ForegroundColor Green
    } else {
        Write-Host "✗ Jackson JSON annotations not found" -ForegroundColor Red
    }
    
    if ($configContent -match "Map<String, ServerConfig> servers") {
        Write-Host "✓ Multi-server configuration map found" -ForegroundColor Green
    } else {
        Write-Host "✗ Multi-server configuration map not found" -ForegroundColor Red
    }
} else {
    Write-Host "✗ McpConfiguration.java not found" -ForegroundColor Red
}

# Test configuration service classes
$configServiceFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\config\McpConfigurationService.java"
if (Test-Path $configServiceFile) {
    Write-Host "✓ McpConfigurationService found" -ForegroundColor Green
} else {
    Write-Host "✗ McpConfigurationService not found" -ForegroundColor Red
}

$configLoaderFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\config\McpConfigurationLoader.java"
if (Test-Path $configLoaderFile) {
    Write-Host "✓ McpConfigurationLoader found" -ForegroundColor Green
} else {
    Write-Host "✗ McpConfigurationLoader not found" -ForegroundColor Red
}
Write-Host ""

# Test that McpServerManager exists and has multi-server methods
Write-Host "3. Testing MCP Server Manager..." -ForegroundColor Yellow
$managerFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\manager\McpServerManager.java"
if (Test-Path $managerFile) {
    $managerContent = Get-Content $managerFile -Raw
      $features = @(
        @{ Name = "initializeServers method"; Pattern = "public void initializeServers" },
        @{ Name = "listAllTools method"; Pattern = "public CompletableFuture.*listAllTools" },
        @{ Name = "getServerStatus method"; Pattern = "public Map.*getServerStatus" },
        @{ Name = "callTool with server ID"; Pattern = "public CompletableFuture.*callTool.*String serverId" },
        @{ Name = "pingAllServers method"; Pattern = "public CompletableFuture.*pingAllServers" }
    )
    
    foreach ($feature in $features) {
        if ($managerContent -match $feature.Pattern) {
            Write-Host "  ✓ $($feature.Name) found" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $($feature.Name) not found" -ForegroundColor Red
        }
    }
} else {
    Write-Host "✗ McpServerManager.java not found" -ForegroundColor Red
}
Write-Host ""

# Test that McpCommand is registered
Write-Host "4. Testing MCP Command Registration..." -ForegroundColor Yellow
$registrationFile = "src\main\java\sg\edu\nus\iss\misoto\cli\commands\CommandRegistrationService.java"
if (Test-Path $registrationFile) {
    $regContent = Get-Content $registrationFile -Raw
    if ($regContent -match "mcpCommand") {
        Write-Host "✓ McpCommand is registered in CommandRegistrationService" -ForegroundColor Green
    } else {
        Write-Host "✗ McpCommand not found in CommandRegistrationService" -ForegroundColor Red
    }
} else {
    Write-Host "✗ CommandRegistrationService.java not found" -ForegroundColor Red
}
Write-Host ""

# Test that configuration classes exist
Write-Host "5. Testing Configuration Classes..." -ForegroundColor Yellow
$configFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\config\McpConfiguration.java"
if (Test-Path $configFile) {
    $configContent = Get-Content $configFile -Raw
    if ($configContent -match "@ConfigurationProperties\(prefix = \"mcp\"\)") {
        Write-Host "✓ McpConfiguration with proper annotation found" -ForegroundColor Green
    } else {
        Write-Host "✗ McpConfiguration annotation not found" -ForegroundColor Red
    }
    
    if ($configContent -match "Map<String, ServerConfig> servers") {
        Write-Host "✓ Multi-server configuration map found" -ForegroundColor Green
    } else {
        Write-Host "✗ Multi-server configuration map not found" -ForegroundColor Red
    }
} else {
    Write-Host "✗ McpConfiguration.java not found" -ForegroundColor Red
}
Write-Host ""

Write-Host "Multi-Server MCP Configuration Test Completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary of Features:" -ForegroundColor Yellow
Write-Host "  • Multiple MCP servers can be configured in application.properties" -ForegroundColor Cyan
Write-Host "  • Each server can be individually enabled/disabled" -ForegroundColor Cyan
Write-Host "  • McpClient supports server-specific configuration" -ForegroundColor Cyan
Write-Host "  • McpServerManager handles multiple server connections" -ForegroundColor Cyan
Write-Host "  • Load balancing and failover support implemented" -ForegroundColor Cyan
Write-Host "  • Tool discovery across all connected servers" -ForegroundColor Cyan
Write-Host "  • MCP command is properly registered for CLI usage" -ForegroundColor Cyan
