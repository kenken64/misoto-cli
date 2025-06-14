# Claude Code CLI - PowerShell Script

# Get the directory where this script is located
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

# Check if Maven wrapper exists
$MavenWrapper = Join-Path $ScriptDir "mvnw.cmd"
if (-not (Test-Path $MavenWrapper)) {
    Write-Error "Maven wrapper not found. Please ensure you're running this from the project root."
    exit 1
}

# Check if the JAR file exists
$JarFile = Join-Path $ScriptDir "target\misoto-0.0.1-SNAPSHOT.jar"
if (-not (Test-Path $JarFile)) {
    Write-Host "Building project..." -ForegroundColor Yellow
    & $MavenWrapper clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) {
        Write-Error "Build failed!"
        exit 1
    }
}

# Run the CLI with all passed arguments
$JavaArgs = @(
    "-jar", $JarFile
) + $args

& java @JavaArgs
