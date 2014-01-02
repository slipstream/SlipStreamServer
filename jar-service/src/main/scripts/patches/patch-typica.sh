#!/bin/bash -e

###
# +=================================================================+
# SlipStream Server (WAR)
# =====
# Copyright (C) 2013 SixSq Sarl (sixsq.com)
# =====
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# 
#      http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# -=================================================================-
###

# Checkout a specific tag of typica. 
# Patch & build it. 
# Install into local Maven repo. 

TAG=1.7.2
REL=${TAG}-1

progname=$(basename $0)
ls $progname >/dev/null 2>&1 || { echo "Run from the directory where the file '$progname' resides."; exit 1; }

CWD=$PWD
PATCH=$CWD/${progname/\.sh/.patch}

function cleanup() {
    rm -rf $TMPDIR
    set +x
}

trap cleanup EXIT

TMPDIR=$(mktemp -d -t typicaXXX)

set -x

cd $TMPDIR
svn checkout http://typica.googlecode.com/svn/tags/v${TAG}/
cd v$TAG/
patch -p0 < $PATCH
echo $REL | ant dist && \
   cp build/jar/typica.jar build/jar/typica-$REL.jar
mvn install:install-file \
  -Dfile=build/jar/typica-$REL.jar -DgroupId=com.google.code.typica \
  -DartifactId=typica -Dversion=$REL -Dpackaging=jar

# update .pom of 1.7.2-1 as typica 1.7.2 requires fixed version 4.0.1 of httpclient
cp -f ~/.m2/repository/com/google/code/typica/typica/1.7.2/typica-1.7.2.pom \
   ~/.m2/repository/com/google/code/typica/typica/1.7.2-1/typica-1.7.2-1.pom
sed -i -e 's/1\.7\.2/1.7.2-1/' \
   ~/.m2/repository/com/google/code/typica/typica/1.7.2-1/typica-1.7.2-1.pom
