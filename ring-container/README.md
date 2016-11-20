
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

To start the service via the REPL, use directly the `start` function:

```
(start 'my.example/app 1234)
```

This will load the namespace "my.example" and execute the
initialization function "app" from that namespace.  It will then start
the service asynchronously on the port "1234".  The function returns a
shutdown function, which must be called to stop the server. The boot
and shell environment (excepting the classpath) will not affect a
server started in this way.

Starting with systemd
---------------------

The ring container is packaged in its own RPM package and can be
reused for many different micro-services.  To use this, the
micro-service must provide a systemd service file similar to the
following:

```
[Unit]
Description=SlipStream Micro-Service
After=syslog.target
After=network.target

[Service]
EnvironmentFile=-/etc/default/slipstream/micro-service

User=slipstream

WorkingDirectory=/opt/slipstream/micro-service

ExecStart=/usr/bin/java \
            -cp "/opt/slipstream/ring-container/slipstream-ring-container.jar:/opt/slipstream/ring-container/lib/*" \
            sixsq.slipstream.server.ring_container.main
ExecStop=/bin/kill -TERM $MAINPID

# When a JVM receives a SIGTERM signal it exits with code 143
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

It is important to preserve the classpath to the ring container and
its dependencies, adding the additional classpath entries for the
micro-service itself.

The systemd service file must reference an environment file located in
`/etc/default/slipstream/` that contains at least the following
variables:

```
SLIPSTREAM_RING_CONTAINER_INIT=my.example/app
SLIPSTREAM_RING_CONTAINER_PORT=1234
```

These provide the required information to find the correct
initialization function and to start the service on the correct port.

Logging
-------

The service assumes that `clojure.tools.logging` will be used with the
SLF4J and log4j implementation.  The package does not provide a
`log4j.properties` file.  This must be provided on the classpath by
each micro-service.
