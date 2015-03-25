#!/bin/sh

./integration-start-servers.sh

./integration-tests.sh

kill -9 $DB_PID $SSCLJ_PID $SS_JAVA_PID

