# Release Process

This document describes how to create releases for Misoto CLI.

## Automated Release Process

The project uses GitHub Actions to automatically build, test, and release JAR files.

### Creating a Release

#### Method 1: Using Git Tags (Recommended)

1. Make sure you're on the main or ollama branch and everything is committed:
   ```bash
   # For main branch releases
   git checkout main
   git pull origin main
   
   # OR for ollama branch releases  
   git checkout ollama
   git pull origin ollama
   ```

2. Use the release script:
   ```bash
   ./scripts/release.sh v1.0.0
   ```

3. The script will:
   - Run tests to ensure everything works
   - Build the project
   - Create and push a git tag
   - Trigger GitHub Actions to create the release

#### Method 2: Manual Tag Creation

1. Create and push a tag manually:
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0"
   git push origin v1.0.0
   ```

#### Method 3: Manual Workflow Dispatch

1. Go to the GitHub Actions tab in your repository
2. Select the "Build, Test and Release" workflow
3. Click "Run workflow"
4. Enter the version (e.g., v1.0.0)
5. Click "Run workflow"

## What Happens During Release

The GitHub Actions workflow will:

1. **Build and Test**:
   - Set up Java 17
   - Run all tests
   - Build the JAR file
   - Generate test reports

2. **Create Release**:
   - Generate comprehensive release notes
   - Create a GitHub release
   - Upload the JAR file as a release asset
   - Include installation instructions
   - Add changelog with commits since last release

## Release Assets

Each release includes:

- `misoto-{version}.jar` - The main executable JAR file
- Comprehensive release notes with:
  - Installation instructions
  - Quick start guide
  - List of features
  - Changelog of commits
  - Contributors list

## Version Numbering

Use semantic versioning (SemVer): `vMAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes
- **MINOR**: New features (backward compatible)
- **PATCH**: Bug fixes (backward compatible)

Examples:
- `v1.0.0` - Initial stable release
- `v1.1.0` - New features added
- `v1.1.1` - Bug fixes
- `v2.0.0` - Breaking changes

## Release Notes

Release notes are automatically generated and include:

- **Installation instructions** with download links
- **Requirements** (Java 17+, API keys)
- **Quick start guide** with example commands
- **Key features** overview
- **Available commands** list
- **Changelog** with commits since last release
- **Contributors** list

## Testing Releases

After a release is created:

1. Download the JAR from the GitHub releases page
2. Test basic functionality:
   ```bash
   java -jar misoto-{version}.jar --help
   java -jar misoto-{version}.jar provider status
   ```

## Rollback Process

If a release has issues:

1. Create a new patch release with fixes
2. Update the `latest` tag to point to the working version:
   ```bash
   git tag -f latest v1.0.1
   git push origin latest --force
   ```

## Pre-release Testing

Before creating a release:

1. Run the full test suite:
   ```bash
   mvn clean test
   ```

2. Test key functionality:
   ```bash
   mvn clean package -DskipTests
   java -jar target/misoto-*.jar provider status
   java -jar target/misoto-*.jar chat
   ```

3. Verify integration tests (if any):
   ```bash
   ./scripts/test-integration.sh  # if exists
   ```

## Continuous Integration

The CI workflow runs automatically on:
- Every push to `main` or `ollama` branches
- Every pull request to `main` or `ollama` branches

This ensures code quality before releases.

## Branch Strategy

- **main**: Stable releases with Anthropic provider focus
- **ollama**: Releases with Ollama provider focus and local AI capabilities

Both `main` and `ollama` branches can be used for releases, allowing for different feature sets or provider configurations.