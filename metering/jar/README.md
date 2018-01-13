
SlipStream Metering Service
===========================

This service performs the metering of a given resource.  By default,
it will generate metering documents for the virtual-machine resources.

The `metering-live-test` tests run only against a live server.  To
start an Elasticsearch server on your local machine you can do the
following from a separate terminal:

    lein start-es

This will start a local server listing on ports 9200 and 9300. You
will have to kill the server when you're finished with the testing.


