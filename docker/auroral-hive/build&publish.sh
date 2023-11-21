#!/bin/bash
USAGE="$(basename "$0") [ -h ] [ -e env ]
-- Build and publish image to docker registry
-- Flags:
      -h  shows help
      -e  environment [ dev (default), prod, ... ]"

# Default configuration
ENV=latest
REGISTRY=acimmino
IMAGE_NAME=wot-hive
#REGISTRY=ghcr.io
#IMAGE_NAME=auroralh2020/wot-hive
PLATFORMS=linux/amd64,linux/arm64,linux/arm/v7

# Maven using docker / local
MAVEN_DOCKER=0

# Get configuration
while getopts 'hd:e:' OPTION; do
case "$OPTION" in
    h)
    echo "$USAGE"
    exit 0
    ;;
    e)
    ENV="$OPTARG"
    ;;
esac
done

# Do login
docker login

# Multiarch builder
docker buildx use multiplatform

# Build for AMD64/ARM64 & push to private registry
docker buildx build --platform ${PLATFORMS} \
                    --tag ${REGISTRY}/${IMAGE_NAME}:${ENV} \
                    --build-arg UID=1001 --build-arg GID=1001 \
                    --build-arg BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
                    --build-arg BUILD_VERSION="1.0" \
                    -f Dockerfile . --push
docker pull ${REGISTRY}/${IMAGE_NAME}:${ENV}