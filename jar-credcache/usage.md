# Credential Cache

This subsystem allows for the management and caching of user
credentials for cloud infrastructures.  It generally follows the
resource schemas and CRUD operations for the [CIMI][cimi] standard.

The CIMI CRUD operations follow the usual REST patterns that use the
HTTP POST, GET, PUT, and DELETE actions.  The only slight difference
is that for some resources (credentials in particular) the create
operation takes a CredentialTemplate resource rather than a Credential
resource.

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

  :host
  :port
  :from 
  :user -- 
  :pass -- password for SMTP connection
  :ssl -- use SSL for SMTP connection



[cimi]: http://dmtf.org/sites/default/files/standards/documents/DSP0263_1.1.0.pdf

