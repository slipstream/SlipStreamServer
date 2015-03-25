#!/bin/sh

WAR_LOCATION=../../../war

echo "Starting DB..."
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar org.hsqldb.server.Server \
        --database.0 file:slipstreamdb \
        --dbname.0 slipstream > /dev/null 2>&1 &

DB_PID=$!

echo "Starting SlipStream Java Server..."
cd $WAR_LOCATION
mvn jetty:run-war -Dpersistence.unit=hsqldb-schema > /dev/null 2>&1 &
SS_JAVA_PID=$!

echo "Starting SlipStream Clojure Server..."
java -cp ../ssclj/jar/target/SlipStreamCljResources-jar-2.5.0-SNAPSHOT-jar-with-dependencies.jar com.sixsq.slipstream.ssclj.app.main 8201 > /dev/null 2>&1 &
SSCLJ_PID=$!

echo "Wait for servers to be started"
sleep 40

echo "Inserting test users..."
java  -Dpersistence.unit=hsqldb-schema \
-cp \
target/SlipStreamServer-war-2.5.0-SNAPSHOT/WEB-INF/lib:\
../jar-persistence/target/SlipStreamPersistence-2.5.0-SNAPSHOT.jar:\
../jar-persistence/target/SlipStreamPersistence-2.5.0-SNAPSHOT-tests.jar:\
../jar-service/target/SlipStreamService-2.5.0-SNAPSHOT-tests.jar\
 com/sixsq/slipstream/event/IntegrationTestHelper 
echo "... DONE"

