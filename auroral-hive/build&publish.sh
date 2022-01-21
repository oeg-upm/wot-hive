#!/bin/bash
USAGE="$(basename "$0") [ -h ] [ -e env ]
-- Build and publish image to docker registry
-- Flags:
      -h  shows help
      -e  environment [ dev (default), prod, ... ]"

# Default configuration
ENV=auroral-dev
REGISTRY=acimmino
IMAGE_NAME=wot-hive
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
# Build Sources
# echo Maven build

# if [ $MAVEN_DOCKER == 1 ]; then 
#     # create volume for maven dependencies
#     docker volume create --name maven-repo
#     # build using maven
#     docker run -it --rm \
#         -v maven-repo:/root/.m2 \
#         -v /Users/peterdrahovsky/Documents/bavenir/wothive/wot-hive:/opt/maven \
#         -w /opt/maven \
#         maven:3.8.4-jdk-11 \
#         mvn clean package
# else
#     mvn clean package
#fi

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