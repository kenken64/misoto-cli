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

$cliOptionsFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\config\McpCliOptions.java"
if (Test-Path $cliOptionsFile) {
    Write-Host "✓ McpCliOptions found" -ForegroundColor Green
} else {
    Write-Host "✗ McpCliOptions not found" -ForegroundColor Red
}
Write-Host ""

# Test that McpServerManager exists and has multi-server methods
Write-Host "3. Testing MCP Server Manager..." -ForegroundColor Yellow
$managerFile = "src\main\java\sg\edu\nus\iss\misoto\cli\mcp\manager\McpServerManager.java"
if (Test-Path $managerFile) {
    $managerContent = Get-Content $managerFile -Raw
    
    $features = @(
        @{ Name = "initializeAll method"; Pattern = "public void initializeAll" },
        @{ Name = "listAllTools method"; Pattern = "public List.*listAllTools" },
        @{ Name = "getServerStatus method"; Pattern = "getServerStatus" },
        @{ Name = "callTool method"; Pattern = "public.*callTool.*String toolName" },
        @{ Name = "pingAll method"; Pattern = "public boolean pingAll" }
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

# Test MCP Command implementation
Write-Host "5. Testing MCP Command Implementation..." -ForegroundColor Yellow
$mcpCommandFile = "src\main\java\sg\edu\nus\iss\misoto\cli\commands\impl\McpCommand.java"
if (Test-Path $mcpCommandFile) {
    $mcpCommandContent = Get-Content $mcpCommandFile -Raw
    
    $commandFeatures = @(
        @{ Name = "handleConfig method"; Pattern = "private void handleConfig" },
        @{ Name = "config show handler"; Pattern = "handleConfigShow" },
        @{ Name = "config load handler"; Pattern = "handleConfigLoad" },
        @{ Name = "config create handler"; Pattern = "handleConfigCreate" },
        @{ Name = "config validate handler"; Pattern = "handleConfigValidate" },
        @{ Name = "config save handler"; Pattern = "handleConfigSave" }
    )
    
    foreach ($feature in $commandFeatures) {
        if ($mcpCommandContent -match $feature.Pattern) {
            Write-Host "  ✓ $($feature.Name) found" -ForegroundColor Green
        } else {
            Write-Host "  ✗ $($feature.Name) not found" -ForegroundColor Red
        }
    }
} else {
    Write-Host "✗ McpCommand.java not found" -ForegroundColor Red
}
Write-Host ""

# Test CLI functionality
Write-Host "6. Testing CLI Functionality..." -ForegroundColor Yellow

# Test if JAR exists
$jarFile = "target\misoto-0.0.1-SNAPSHOT.jar"
if (Test-Path $jarFile) {
    Write-Host "✓ JAR file found" -ForegroundColor Green
    
    # Test configuration commands
    Write-Host "  Testing MCP config commands..." -ForegroundColor Cyan
    
    try {
        # Test config show command
        $output = & java -jar $jarFile mcp config show 2>&1
        if ($output -match "Current MCP Configuration") {
            Write-Host "    ✓ 'mcp config show' command works" -ForegroundColor Green
        } else {
            Write-Host "    ✗ 'mcp config show' command failed" -ForegroundColor Red
        }
    } catch {
        Write-Host "    ✗ Error testing CLI commands: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "✗ JAR file not found. Run 'mvn clean package' first." -ForegroundColor Red
}
Write-Host ""

Write-Host "MCP JSON Configuration Migration Test Completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Summary of New Features:" -ForegroundColor Yellow
Write-Host "  • JSON-based configuration replaces application.properties" -ForegroundColor Cyan
Write-Host "  • CLI commands for configuration management (show, load, create, validate, save)" -ForegroundColor Cyan
Write-Host "  • Runtime configuration switching with --mcp-config option" -ForegroundColor Cyan
Write-Host "  • Multiple MCP servers with individual enable/disable" -ForegroundColor Cyan
Write-Host "  • McpConfigurationService for centralized config management" -ForegroundColor Cyan
Write-Host "  • Jackson JSON serialization/deserialization" -ForegroundColor Cyan
Write-Host "  • Configuration validation and error handling" -ForegroundColor Cyan
Write-Host "  • Environment-specific configurations independent of Spring config" -ForegroundColor Cyan
Write-Host ""
Write-Host "Available Commands:" -ForegroundColor Yellow
Write-Host "  java -jar target\misoto-0.0.1-SNAPSHOT.jar mcp config show" -ForegroundColor Cyan
Write-Host "  java -jar target\misoto-0.0.1-SNAPSHOT.jar mcp config create [file]" -ForegroundColor Cyan
Write-Host "  java -jar target\misoto-0.0.1-SNAPSHOT.jar mcp config validate [file]" -ForegroundColor Cyan
Write-Host "  java -jar target\misoto-0.0.1-SNAPSHOT.jar --mcp-config custom.json mcp status" -ForegroundColor Cyan
