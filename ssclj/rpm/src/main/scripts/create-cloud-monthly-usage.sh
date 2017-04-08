#!/bin/bash

java -Des.host=localhost \
     -Des.port=9300 \
     -Dconfig.name=db.spec \
     -cp "/opt/slipstream/ssclj/resources:/opt/slipstream/ssclj/lib/ext/*:/opt/slipstream/ssclj/lib/ssclj.jar" \
     com.sixsq.slipstream.ssclj.usage.summarizer -f monthly -g cloud
