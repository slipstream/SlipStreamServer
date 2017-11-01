#!/bin/bash

[ -f /etc/default/ssclj ] && source /etc/default/ssclj

java -Des.host=${ES_HOST-localhost} \
     -Des.port=${ES_PORT-9300} \
     -Dconfig.name=db.spec \
     -cp "/opt/slipstream/ssclj/resources:/opt/slipstream/cimi-tools/lib/ext/*:/opt/slipstream/ssclj/lib/ssclj.jar" \
     com.sixsq.slipstream.ssclj.usage.summarizer $@
