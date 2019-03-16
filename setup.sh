#!/bin/bash

./gradlew build test jacocoTestReport docker

docker-compose --file ./docker/docker-compose.yml up -d

docker-compose --file ./docker/docker-compose.yml logs -f


