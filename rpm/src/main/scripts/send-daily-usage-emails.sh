#!/bin/bash

/usr/lib/jvm/jre-1.8.0/bin/java -Dpersistence.unit=hsqldb-schema-no-ddl-update \
     -cp "/opt/slipstream/server/webapps/slipstream.war/WEB-INF/lib/*":"/opt/slipstream/server/lib/connectors/*" \
     com.sixsq.slipstream.action.DailyUsageSender
