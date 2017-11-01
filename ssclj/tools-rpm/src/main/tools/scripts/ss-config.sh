#!/usr/bin/env bash

CONF=/etc/default/ssclj-tools-cli
[ -f $CONF ] || { >&2 echo "Missing required config file" $CONF; exit 1; }
source $CONF

java -cp $CLASSPATH com.sixsq.slipstream.tools.cli.ssconfig $@

