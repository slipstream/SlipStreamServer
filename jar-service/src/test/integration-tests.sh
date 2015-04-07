
# $1 expected, $2 actual, $3 ok_msg
function assert_code {
	if [[ $1 == $2 ]]; then
		echo "OK: " $3
	else
		echo
		echo "KAPUTT :-/"		
		echo "KAPUTT :-/"
		echo
		echo "test KO, failed to verify:" $3
		echo "expected "$1" got "$2
		echo
		echo "KAPUTT :-/"		
		echo "KAPUTT :-/"
		echo
		exit 1
	fi
}

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null http://localhost:8080/event/)
assert_code 401 $code "event resource is guarded and authentication is needed"	

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null http://localhost:8080/event/ -uuser1:123456)
assert_code 200 $code "event resource can be accessed by authenticated user"	

#
# POST
#
code=$(curl -H "Content-Type: application/json" -X POST -d "{\"acl\": {\"owner\": {\"type\": \"USER\", \"principal\": \"user1\"},\"rules\": [{\"type\": \"USER\", \"principal\": \"user1\", \"right\": \"ALL\"}]},    \"id\": \"123\",    \"created\" :  \"2015-01-16T08:20:00.0Z\",    \"updated\" : \"2015-01-16T08:20:00.0Z\",    \"resourceURI\" : \"http://slipstream.sixsq.com/ssclj/1/Event\",    \"timestamp\": \"2015-01-10T08:20:00.0Z\",    \"content\" :  { \"resource\":  {\"href\": \"Run/45614147-aed1-4a24-889d-6365b0b1f2cd\"},    \"state\" : \"Started\" } ,    \"type\": \"state\",    \"severity\": \"medium\"}" --write-out "%{http_code}\n" --silent --output /dev/null http://localhost:8080/event/ -uuser1:123456)
assert_code 201 $code "event resource can be created with a json content"

# Retrieve the URL for the last posted Event
event_ref=$(curl --silent http://localhost:8080/event/ -uuser1:123456 | grep "Event\/" | tail -1 | awk '{print $3;}' | cut -d "/" -f 2 | tr -d \")
url="http://localhost:8080/event/"$event_ref

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser1:123456)
assert_code 200 $code "specific event resource can be retrieved by its owner"

code=$(curl --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser2:456789)
assert_code 403 $code "specific event resource can *NOT* be retrieved by another authenticated user"

code=$(curl -X PUT --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser1:123456)
assert_code 405 $code "specific event resource can *NOT* be modified"

code=$(curl -X DELETE --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser2:456789)
assert_code 403 $code "specific event resource can *NOT* be deleted by another authenticated user"

code=$(curl -X DELETE --write-out "%{http_code}\n" --silent --output /dev/null $url -uuser1:123456)
assert_code 204 $code "specific event resource can be deleted by its owner"
