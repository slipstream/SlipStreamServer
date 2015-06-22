#!/bin/bash

java -Dpersistence.unit=hsqldb-schema \
     -cp "/opt/slipstream/server/webapps/slipstream.war/WEB-INF/lib/*": \
     com.sixsq.slipstream.action.DailyUsageSender
