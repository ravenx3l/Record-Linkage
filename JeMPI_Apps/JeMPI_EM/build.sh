#!/bin/bash

set -e
set -u

source ../../docker/conf/images/conf-app-images.sh
source ../build-check-jdk.sh

JAR_FILE=${EM_JAR}
APP_IMAGE=${EM_IMAGE}
APP=em
 
source ../build-app.sh
