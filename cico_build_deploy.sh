#!/bin/bash

###
# #%L
# che-starter
# %%
# Copyright (C) 2017 Red Hat, Inc.
# %%
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
# #L%
###
cat jenkins-env | grep -e RHCHEBOT_DOCKER_HUB_PASSWORD -e GIT -e DEVSHIFT -e KEYCLOAK_TOKEN > inherit-env
. inherit-env

yum -y update
yum -y install centos-release-scl java-1.8.0-openjdk-devel docker curl
yum -y install rh-maven33

# installing jq via curl since 'No package jq available' for yum
curl -LO https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64
mv jq-linux64 /usr/bin/jq
chmod +x /usr/bin/jq

# TARGET variable gives ability to switch context for building rhel based images, default is "centos"
# If CI slave is configured with TARGET="rhel" RHEL based images should be generated then.
TARGET=${TARGET:-"centos"}

# Keycloak token provided by `che_functional_tests_credentials_wrapper` from `openshiftio-cico-jobs` is a refresh token. 
# Obtaining osio user token
AUTH_RESPONSE=$(curl -H "Content-Type: application/json" -X POST -d '{"refresh_token":"'$KEYCLOAK_TOKEN'"}' https://auth.prod-preview.openshift.io/api/token/refresh)

# `OSIO_USER_TOKEN` is used for che-starter integration tests which are run against prod-preview
export OSIO_USER_TOKEN=$(echo $AUTH_RESPONSE | jq --raw-output ".token | .access_token")

systemctl start docker

scl enable rh-maven33 'mvn clean verify -B'

if [ $? -eq 0 ]; then

  if [ $TARGET == "rhel" ]; then
    export DOCKER_REGISTRY=${DOCKER_REGISTRY:-"prod.registry.devshift.net"}
    export DOCKERFILE="Dockerfile.rhel"
    export DOCKER_IMAGE="${DOCKER_REGISTRY}/rhche/che-starter"
  else
    export DOCKERFILE="Dockerfile"
    export DOCKER_IMAGE="rhche/che-starter"
    export REGISTRY="push.registry.devshift.net"
  fi

  export PROJECT_VERSION=`mvn -o help:evaluate -Dexpression=project.version | grep -e '^[[:digit:]]'`

  docker build -t ${DOCKER_IMAGE}:latest -f ${DOCKERFILE} .

  if [ $? -ne 0 ]; then
    echo 'Docker Build Failed!'
    exit 2
  fi

  TAG=$(echo $GIT_COMMIT | cut -c1-${DEVSHIFT_TAG_LEN})

  if [ $TARGET != "rhel" ]; then
    docker login -u rhchebot -p $RHCHEBOT_DOCKER_HUB_PASSWORD -e noreply@redhat.com
  fi

  docker tag ${DOCKER_IMAGE}:latest ${DOCKER_IMAGE}:$TAG
  docker push ${DOCKER_IMAGE}:latest
  docker push ${DOCKER_IMAGE}:$TAG

  if [ $TARGET != "rhel" ]; then
    if [ -n "${DEVSHIFT_USERNAME}" -a -n "${DEVSHIFT_PASSWORD}" ]; then
      docker login -u ${DEVSHIFT_USERNAME} -p ${DEVSHIFT_PASSWORD} ${REGISTRY}
    else
      echo "Could not login, missing credentials for the registry"
    fi
    docker tag ${DOCKER_IMAGE}:latest ${REGISTRY}/almighty/che-starter:$TAG
    docker push ${REGISTRY}/almighty/che-starter:$TAG

    docker tag ${DOCKER_IMAGE}:latest ${REGISTRY}/almighty/che-starter:latest
    docker push ${REGISTRY}/almighty/che-starter:latest
  fi
else
  echo 'Build Failed!'
  exit 1
fi
