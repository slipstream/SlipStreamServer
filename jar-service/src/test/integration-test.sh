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

echo "Will test after servers have been started"
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

# $1 expected, $2 actual, $3 ok_msg
function assert_code {
	if [[ $1 == $2 ]]; then
		echo "OK: " $3
	else
		echo "test KO, failed to verify:" $3
		echo "expected "$1" got "$2
		exit 1
	fi
}

echo "Testing..."

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null http://localhost:8080/event/)
assert_code 401 $code "event resource is guarded and authentication is needed"	

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null http://localhost:8080/event/ -uuser1:123456)
assert_code 200 $code "event resource can be accessed by authenticated user"	

code=$(curl -H "Content-Type: application/json" -X POST -d "{\"acl\": {\"owner\": {\"type\": \"USER\", \"principal\": \"joe\"},\"rules\": [{\"type\": \"ROLE\", \"principal\": \"ANON\", \"right\": \"ALL\"}]},    \"id\": \"123\",    \"created\" :  \"2015-01-16T08:20:00.0Z\",    \"updated\" : \"2015-01-16T08:20:00.0Z\",    \"resourceURI\" : \"http://slipstream.sixsq.com/ssclj/1/Event\",    \"timestamp\": \"2015-01-10T08:20:00.0Z\",    \"content\" :  { \"resource\":  {\"href\": \"Run/45614147-aed1-4a24-889d-6365b0b1f2cd\"},    \"state\" : \"Started\" } ,    \"type\": \"state\",    \"severity\": \"medium\"}" --write-out "%{http_code}\n" --silent --output /dev/null http://localhost:8080/event/ -uuser1:123456)
assert_code 201 $code "event resource can be created with a json content"

# Retrieve the URL for the last posted Event
event_ref=$(curl --silent http://localhost:8080/event/ -uuser1:123456 | grep "Event\/" | tail -1 | awk '{print $3;}' | cut -d "/" -f 2 | tr -d \")
url="http://localhost:8080/event/"$event_ref

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser1:123456)
assert_code 200 $code "specific event resource can be retrieved"

code=$(curl -X PUT --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser1:123456)
assert_code 405 $code "specific event resource can *not* be modified"

code=$(curl -X DELETE --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser1:123456)
assert_code 204 $code "specific event resource can be deleted"

kill -9 $DB_PID $SSCLJ_PID $SS_JAVA_PID

