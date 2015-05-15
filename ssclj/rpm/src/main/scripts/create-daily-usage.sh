#!/bin/bash

java -Dlogfile.path=daily \
     -Ddb.config.path=/opt/slipstream/ssclj/etc/db.spec \
     -cp /opt/slipstream/ssclj/lib/ssclj.jar \
     com.sixsq.slipstream.ssclj.usage.daily_summary_launcher
