
SlipStream Ring Container
=========================

This module contains a simple, generic ring application container that
can be reused for the collection of micro-services that make up the
SlipStream platform.

Initialization and Finalization Functions
-----------------------------------------

To make use of this ring application container, the micro-service
must provide a single initialization function that:

 * Takes no arguments
 * Initializes the micro-service
 * Returns a two-element tuple containing the ring handler and an
   optional finalization function

All of the namespace loading is done dynamically, so the micro-service
does not need AOT compilation.

The optional finalization function will be called prior to shutting
down the micro-service.  It must:

 * Take no arguments
 * Release/close resources for the micro-service
 * Not block the shutdown process

Exceptions from the finalization script will be caught, logged, and
then ignored.  

Starting with the REPL
----------------------

To start the service via the REPL, directly use the `start` function:

```
(require '[sixsq.slipstream.server.ring-container :as rc])
(def stop (rc/start 'sixsq.slipstream.server.ring-example/init 5000))
;; service on  http://localhost:5000 should show "Ring Example Running!"
(stop)
```

This will load the namespace "sixsq.slipstream.server.ring-example"
and execute the initialization function "init" from that namespace.
It will then start the service asynchronously on the port "5000".  The
function returns a shutdown function, which must be called to stop the
server. The boot and shell environment (excepting the classpath) will
not affect a server started in this way.

Starting with systemd
---------------------

The ring container is packaged in its own RPM package and can be
reused for many different micro-services.  To use this, the
micro-service must provide a systemd service file and a defaults file
to provide the variables:

```
SLIPSTREAM_RING_CONTAINER_INIT=my.example/app
SLIPSTREAM_RING_CONTAINER_PORT=1234
```

See the packaged RPM for examples of these files for the simple
example micro-service.

Logging
-------

The service assumes that `clojure.tools.logging` will be used with the
SLF4J and log4j implementation.  The package does not provide a
`log4j.properties` file.  This must be provided on the classpath by
each micro-service.
