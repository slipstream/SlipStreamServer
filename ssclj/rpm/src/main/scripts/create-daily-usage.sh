#!/bin/bash

java -Dconfig.path=db.spec \
     -cp "/opt/slipstream/ssclj/resources:/opt/slipstream/ssclj/lib/ext/*:/opt/slipstream/ssclj/lib/ssclj.jar" \
     com.sixsq.slipstream.ssclj.usage.daily_summary_launcher
