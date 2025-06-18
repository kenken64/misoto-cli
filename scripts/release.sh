#!/bin/bash

# Release script for Misoto CLI
# Usage: ./scripts/release.sh <version>
# Example: ./scripts/release.sh v1.0.0

set -e

if [ -z "$1" ]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 v1.0.0"
    exit 1
fi

VERSION=$1

# Validate version format
if [[ ! $VERSION =~ ^v[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "Error: Version must be in format vX.Y.Z (e.g., v1.0.0)"
    exit 1
fi

echo "🚀 Preparing release $VERSION"

# Check if we're on a release branch
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" != "main" ] && [ "$CURRENT_BRANCH" != "ollama" ]; then
    echo "⚠️  Warning: You're not on main or ollama branch (currently on $CURRENT_BRANCH)"
    read -p "Do you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
elif [ "$CURRENT_BRANCH" = "ollama" ]; then
    echo "📋 Note: Creating release from ollama branch"
fi

# Check if working directory is clean
if [ -n "$(git status --porcelain)" ]; then
    echo "❌ Error: Working directory is not clean. Please commit or stash your changes."
    git status --short
    exit 1
fi

# Pull latest changes
echo "📥 Pulling latest changes..."
git pull origin $CURRENT_BRANCH

# Run tests to make sure everything is working
echo "🧪 Running tests..."
mvn clean test

# Build the project
echo "🔨 Building project..."
mvn clean package -DskipTests

# Create and push tag
echo "🏷️  Creating tag $VERSION..."
git tag -a $VERSION -m "Release $VERSION"

echo "📤 Pushing tag to remote..."
git push origin $VERSION

echo "✅ Release $VERSION has been tagged and pushed!"
echo "🚀 GitHub Actions will automatically create the release."
echo "📋 Check the Actions tab at: https://github.com/$(git config --get remote.origin.url | sed 's/.*github.com[:\/]\([^\/]*\/[^\/]*\)\.git/\1/')/actions"