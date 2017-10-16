SSCLJ Test Server
=================

This repository creates a small jar file that allows an SSCLJ
test server to be started from Java (in addition to the core server
jar file).

The standard `ssclj.jar` file (and transitive dependencies) must be on
the classpath so that all the resource definitions can be found and
loaded into the test server.  The test server should not need any
other dependencies.

To start the server, import the class:

    ssclj.jar.test.com.sixsq.slipstream.ssclj.app.SscljTestServer

You can then call the **static** methods `start` and `stop` to start
and stop the server, respectively. The following services will be
running on the ports:

 - 12001: zookeeper
 - 12002: elasticsearch
 - 12003: ssclj

You can test that the SSCLJ server is responding correctly by
requesting the cloud entry point:

    curl http://localhost:12003/api/cloud-entry-point

It should respond with the typical cloud entry point document. Check
that all the resources have been loaded into the server.

**NOTE:** The hsqldb database is **not** started.  As long as you
avoid username/password authentication with the server, this should
not be a problem.  Use the internal authentication header
(`slipstream-authn-info`) to work around this (easily).
  
