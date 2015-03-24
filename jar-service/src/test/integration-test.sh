#!/bin/sh

function space {
	echo
	echo
	echo
	echo
	echo
	echo
}

WAR_LOCATION=../../../war

space
echo "Starting DB"
java -cp ~/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar org.hsqldb.server.Server \
        --database.0 file:slipstreamdb \
        --dbname.0 slipstream &

space
echo "Starting SlipStream Java Server..."
cd $WAR_LOCATION
mvn jetty:run-war -Dpersistence.unit=hsqldb-schema & > /dev/null 2>&1 &

space
echo "Starting SlipStream Clojure Server..."
java -cp ../ssclj/jar/target/SlipStreamCljResources-jar-2.5.0-SNAPSHOT-jar-with-dependencies.jar com.sixsq.slipstream.ssclj.app.main 8201 > /dev/null 2>&1 &

space
echo "Will test after servers are started"
sleep 40

space
echo "Inserting test users..."
java  -Dpersistence.unit=hsqldb-schema \
-cp \
/Users/st/.m2/repository/org/hibernate/javax/persistence/hibernate-jpa-2.1-api/1.0.0.Final/hibernate-jpa-2.1-api-1.0.0.Final.jar:\
/Users/st/.m2/repository/org/hibernate/common/hibernate-commons-annotations/4.0.4.Final/hibernate-commons-annotations-4.0.4.Final.jar:\
/Users/st/.m2/repository/org/hibernate/hibernate-entitymanager/4.3.5.Final/hibernate-entitymanager-4.3.5.Final.jar:\
/Users/st/.m2/repository/org/hibernate/hibernate-core/4.3.5.Final/hibernate-core-4.3.5.Final.jar:\
/Users/st/.m2/repository/org/hibernate/hibernate-c3p0/4.3.5.Final/hibernate-c3p0-4.3.5.Final.jar:\
/Users/st/.m2/repository/org/jboss/logging/jboss-logging/3.1.3.GA/jboss-logging-3.1.3.GA.jar:\
/Users/st/.m2/repository/org/jboss/jandex/1.1.0.Final/jandex-1.1.0.Final.jar:\
/Users/st/.m2/repository/org/jboss/spec/javax/transaction/jboss-transaction-api_1.2_spec/1.0.0.Final/jboss-transaction-api_1.2_spec-1.0.0.Final.jar:\
/Users/st/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar:\
/Users/st/.m2/repository/javax/mail/mail/1.4.1/mail-1.4.1.jar:\
/Users/st/.m2/repository/dom4j/dom4j/1.6.1/dom4j-1.6.1.jar:\
/Users/st/.m2/repository/org/hsqldb/hsqldb/2.3.2/hsqldb-2.3.2.jar:\
/Users/st/.m2/repository/org/restlet/jee/org.restlet/2.2.1/org.restlet-2.2.1.jar:\
/Users/st/.m2/repository/org/restlet/jee/org.restlet.ext.servlet/2.2.1/org.restlet.ext.servlet-2.2.1.jar:\
/Users/st/.m2/repository/org/restlet/jee/org.restlet.ext.xml/2.2.1/org.restlet.ext.xml-2.2.1.jar:\
/Users/st/.m2/repository/antlr/antlr/2.7.7/antlr-2.7.7.jar:\
target/SlipStreamServer-war-2.5.0-SNAPSHOT/WEB-INF/lib:\
../jar-persistence/target/SlipStreamPersistence-2.5.0-SNAPSHOT.jar:\
../jar-persistence/target/SlipStreamPersistence-2.5.0-SNAPSHOT-tests.jar:\
../jar-service/target/SlipStreamService-2.5.0-SNAPSHOT-tests.jar\
 com/sixsq/slipstream/event/IntegrationTestHelper

# $1 expected, $2 actual, $3 ok_msg
function assert_code {
	if [[ $1 == $2 ]]; then
		echo $3
	else
		echo "test KO, failed to verify:" $3
		echo "expected "$1" got "$2
		exit 1
	fi
}

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



