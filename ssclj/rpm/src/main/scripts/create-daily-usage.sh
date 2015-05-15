#!/bin/bash

java -Ddb.config.path=db.spec \
     -cp /opt/slipstream/ssclj/etc:/opt/slipstream/ssclj/lib/ssclj.jar \
     com.sixsq.slipstream.ssclj.usage.daily_summary_launcher
