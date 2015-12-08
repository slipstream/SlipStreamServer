#!/bin/bash -e

PID_FILE=$(dirname $0)/ssclj-server.pid
[ -f $PID_FILE ] && \
    { kill $(cat $PID_FILE) || \
            { rc=$?; rm -f $PID_FILE; exit $rc; }  && \
                { echo "SSCLJ server stopped."; rm -f $PID_FILE; exit 0; }; } || \
        { echo "pid file $PID_FILE not present."; exit 1; }

