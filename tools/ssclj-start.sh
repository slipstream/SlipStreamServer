#!/bin/bash -e

SSCLJ_PORT=${1-8201}
CONFIG_FILE=${2-config-db.edn}

_DIRNAME=$(dirname $0)
PID_FILE=$_DIRNAME/ssclj-server.pid

[ "x$_DIRNAME" == "x." ] && \
    _BASE='./..' || \
    _BASE='.'

SSCLJ_JAR=$(find $_BASE/ssclj/jar/ -name 'SlipStreamCljResources-jar-*-jar-with-dependencies.jar')
java \
    -Dconfig.path=$CONFIG_FILE \
    -Dlogfile.path=dev \
    -cp $_BASE/ssclj/jar/src/main/resources:$SSCLJ_JAR \
    com.sixsq.slipstream.ssclj.app.main \
    $SSCLJ_PORT & echo $! > $PID_FILE
echo "SSCLJ server started: port $SSCLJ_PORT, pid $(cat $PID_FILE)."

