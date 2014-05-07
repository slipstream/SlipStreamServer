# Credential Cache

This subsystem allows for the management and caching of user
credentials for cloud infrastructures.  It generally follows the
resource schemas and CRUD operations for the [CIMI][cimi] standard.

The CIMI CRUD operations follow the usual REST patterns that use the
HTTP POST, GET, PUT, and DELETE actions.  The only slight difference
is that for some resources (credentials in particular) the create
operation takes a CredentialTemplate resource rather than a Credential
resource.

## User Workflow

This was developed primarily to allow users to delegate a time-limited
proxy to the SlipStream server via the MyProxy/VOMS servers used
within EGI.  The workflow for users is the following:

  * Users must create a short-lived credential on the MyProxy server
    protected with a temporary username and password.  This can be
    done with the command `myproxy -l random_username -c 1`.

  * User must also create a long-lived credential on the MyProxy
    server that allows renewals by holders of a valid proxy.  This can
    be done with the command `myproxy-init -d -R 'full DN, globus
    fmt'`.

  * The user then edits their SlipStream user parameters, providing
    the temporary username/password of the short-lived credential.
    SlipStrema will then retrieve this credential and delete it from
    the MyProxy server.

  * The user must maintain an active long-lived credential on the
    MyProxy server to allow credential renewals.  The server will
    attempt renewals until the credential validity falls below 6
    minutes.  Users are notified of failure.

  * If the credential on the server expires, the user must delegate a
    new credential to the server via this procedure.

If the user provides VOMS information, then the credential cache will
also create proxies that contain VOMS attribute certificates.

## SlipStream Integration

The main entry point for the system from clojure is the namespace:

    slipstream.credcache.control

where the functions `start!` and `stop!` must be called when
SlipStream is started and stopped, respectively.  It is extremely
important that the `stop!` function be called on termination; if it is
not called daemon threads for the job scheduling will not be stopped
causing the JVM to hang.

The `start!` function takes two map arguments: cb-params and
stmp-params.  These contain the Couchbase client parameters and SMTP
parameters for the subsystem.

The Couchbase parameters are:

  :uris -- list of URIs for Couchbase instances
  :bucket -- Couchbase bucket to use
  :username -- username to access bucket
  :password -- password to access bucket

If these are not specified, then the default bucket on the local
machine will be used. 

The SMTP parameters are:

  :host -- host name of the SMTP connection
  :port -- (opt.) port for SMTP connection
  :from -- (opt.) defaults to user@host
  :user -- username for SMTP connection
  :pass -- password for SMTP connection
  :ssl -- (opt.) use SSL for SMTP connection

If these are not specified, then users will not be notified when
credential renewal fails.

A wrapper class has also been created that makes it easier to use the
core features of the system from Java.  This class is
`slipstream.credcache.javawrapper` and contains four static methods:

  * void start(Map couchbase, Map smtp): must be called at the
    SlipStream server start
  * void stop(): must be called before stopping the SlipStream server
  * String create(Map template): does the initial delegation of a
    proxy to the server.  See the MyProxyVomsCredentialTemplate value
    for the schema of the template.
  * File retrieve(String id): Retrieve the given proxy and writes it
    to a temporary file.  The caller is responsible for deleting the
    temporary file.

Integration of the MyProxy/VOMS functionality will also require that
the EGI certificates and VOMS servers are configured correctly on the
SlipStream server.

This integration has not yet be included to allow for the adding the
new dependencies and PKI configuration to the server.

[cimi]: http://dmtf.org/sites/default/files/standards/documents/DSP0263_1.1.0.pdf

