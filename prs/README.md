# pricing-rest-service

A simple rest server which uses the pricing lib and the service
catalog (129.194.184.194)

## Prerequisites

The service depends on Java 8.  You must a certified distribution,
usually either Oracle or OpenJDK.  Note the the GNU java
implementation will **not** work. 

To build the software you will need to have
[boot][http://boot-clj.com] installed.  To run tests, execute:

```
$ boot run-tests
```

This will perform a complete compilation of the code and then execute
any defined unit tests.

To create the uberjar for the pricing service, execute:

```
$ boot build target
```

The `build` will create the jar file and `target` will save it to the
local `target` subdirectory.

The build can also be triggered with
[maven][https://maven.apache.org].  In this case, the build and test
dance can be done with:

```
$ mvn clean install
```

The `clean` is optional.

## Running

The service can be started in several ways.  If you have generated the
uber-jarfile, then you can start the service like so:

```
$ export SS_PORT=8888
$ java -jar target/SlipStreamPricingService-jar-3.3-SNAPSHOT.jar
```

This will start the server on the specified port.  If the
environmental variable is not defined, then the service will default
to port 15000.

To start the service within the REPL, the easiest is the following:

```
$ boot repl

boot.user=> (require '[sixsq.slipstream.pricing.service.server :as s])
boot.user=> (def shutdown (s/start 8082))
boot.user=> ... do something ...
boot.user=> (shutdown)
```

The service can be started/stopped multiple times in a REPL session.
**However if you change the routing, then you'll need to restart the
REPL.** Compojure uses a `defonce` for the routing table and it won't
be reloaded with a simple restart, not even with a :reload-all on the
namespace.

To start the service with boot and to restart on changes, you can do
the following:

```
$ boot run
```

To stop the process you'll need to type ctrl-c. 

## Copyright

Copyright © 2016 SixSq Sarl. All rights reserved.
