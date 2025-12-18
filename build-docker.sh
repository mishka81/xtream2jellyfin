#!/bin/bash

# Build script for multi-platform Docker images using buildx
# Usage: ./build-docker.sh [version] [platforms] [push]
#
# Examples:
#   ./build-docker.sh                           # Build for local platform only (version from pom.xml)
#   ./build-docker.sh 1.0.0                     # Build for local platform with specific version
#   ./build-docker.sh 1.0.0 multi               # Build for multiple platforms
#   ./build-docker.sh 1.0.0 multi push          # Build and push to registry

set -eou pipefail

# Extract version from pom.xml if not provided
if [ -z "${1:-}" ]; then
    VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    echo "Using version from pom.xml: ${VERSION}"
else
    VERSION=$1
fi

PLATFORMS="linux/amd64"
PUSH_FLAG=""
ACTION="load"

# Check if multi-platform build requested
if [ "${2:-}" == "multi" ]; then
    PLATFORMS="linux/amd64,linux/arm64,linux/arm/v7"
    ACTION="load"
    echo "Building for multiple platforms: ${PLATFORMS}"
fi

# Check if push requested
if [ "${3:-}" == "push" ] || [ "${2:-}" == "push" ]; then
    PUSH_FLAG="--push"
    ACTION=""
    echo "Will push to registry"
fi

# If pushing, we can't use load
if [ -n "$PUSH_FLAG" ]; then
    ACTION=""
else
    # For multi-platform without push, we can't load (limitation of buildx)
    if [ "${2:-}" == "multi" ]; then
        echo "Warning: Multi-platform builds without push will only be cached, not loaded into Docker"
        ACTION=""
    fi
fi

IMAGE_NAME="xtream2jellyfin"
IMAGE_TAG="${IMAGE_NAME}:${VERSION}"
IMAGE_LATEST="${IMAGE_NAME}:latest"

echo "Building Docker image:"
echo "  Image: ${IMAGE_TAG}"
echo "  Platforms: ${PLATFORMS}"
echo "  Version: ${VERSION}"

# Build command
BUILD_CMD="docker buildx build \
    --platform ${PLATFORMS} \
    --build-arg VERSION=${VERSION} \
    -t ${IMAGE_TAG} \
    -t ${IMAGE_LATEST} \
    ${PUSH_FLAG}"

# Add load flag if not pushing and single platform
if [ -z "$PUSH_FLAG" ] && [ "${2:-}" != "multi" ]; then
    BUILD_CMD="${BUILD_CMD} --load"
fi

BUILD_CMD="${BUILD_CMD} ."

echo ""
echo "Executing: ${BUILD_CMD}"
echo ""

eval ${BUILD_CMD}

echo ""
echo "Build complete!"

if [ -z "$PUSH_FLAG" ] && [ "${2:-}" != "multi" ]; then
    echo "Image loaded: ${IMAGE_TAG}"
    echo "Image loaded: ${IMAGE_LATEST}"
elif [ -n "$PUSH_FLAG" ]; then
    echo "Image pushed: ${IMAGE_TAG}"
    echo "Image pushed: ${IMAGE_LATEST}"
else
    echo "Image cached (use 'push' flag to push to registry)"
fi
