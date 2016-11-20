(ns sixsq.slipstream.server.ring-container
  "Provides a simple, generic ring application server that can be started
   either via the static 'main' function (e.g. for running as a daemon) or via
   the 'start' function (e.g. for testing from the REPL)."
  (:require
    [clojure.tools.logging :as log])
  (:import
    [java.io Closeable]
    [java.net InetSocketAddress])
  (:gen-class))

(def ^:const default-port 8200)

(defn- dyn-resolve
  "Dynamically requires the namespace of the given symbol and then resolves
   the name of the symbol in this namespace. Returns the var for the resolved
   symbol. Throws an exception if the symbol could not be resolved."
  [s]
  (try
    (let [[ns f] (->> s
                      symbol
                      ((juxt namespace name))
                      (map symbol))]
      (require ns)
      (if-let [result (ns-resolve (find-ns ns) f)]
        result
        (let [msg (str "symbol (" s ") could not be resolved")]
          (log/error msg)
          (throw (ex-info msg {})))))
    (catch Exception e
      (let [msg (str "error requiring or resolving symbol ( " s " )\n" (.getMessage e))]
        (log/error msg)
        (throw (ex-info msg {}))))))

(defn- hook
  "Creates a JVM shutdown hook that runs the given stopping function. Note
   that some logging may be lost depending on the state of the JVM when the
   hook is run."
  [shutdown-fn]
  (proxy [Thread] []
    (run [] (shutdown-fn))))

(defn- register-shutdown-hook
  "Registers a shutdown hook in the JVM to shutdown the application and
   application container cleanly."
  [shutdown-fn]
  (.. (Runtime/getRuntime)
      (addShutdownHook (hook shutdown-fn))))

(defn- create-shutdown-fn
  [^Closeable server finalization-fn]
  (fn [] (try
           (and finalization-fn (finalization-fn))
           (log/info "successfully finalized webapp")
           (catch Exception e
             (log/error "failure when finalizing webapp:" (str e)))
           (finally
             (try
               (and server (.close server))
               (log/info "successfully shutdown webapp")
               (catch Exception e
                 (log/error "failure when shutting down webapp:" (str e))))))))

(defn- start-container
  "Starts the aleph container with the given ring handler and on the given
   port. Returns the server object that has been started, which can be stopped
   by calling 'close' on the object."
  [handler ^long port]
  (log/info "starting aleph application container on port" port)
  (let [start-server (dyn-resolve 'aleph.http/start-server)]
    (log/info "started aleph application container on port" port)
    (->> port
         (InetSocketAddress. "127.0.0.1")
         (hash-map :socket-address)
         (start-server handler))))

(defn- parse-port
  "Parses the given value (string or int) as an integer and returns the value
   if it is a valid port number. For any invalid input, this function returns
   the default port."
  [s]
  (try
    (let [port (cond
                 (string? s) (Integer/valueOf ^String s)
                 (integer? s) (Integer/valueOf ^int s)
                 :else default-port)]
      (if (< 0 port 65536)
        port
        default-port))
    (catch Exception _
      default-port)))

(defn start
  "Starts the application and application server. Return a 'stop' function
   that will shutdown the application and server when called."
  [server-init & [port]]
  (let [server-init-fn (dyn-resolve server-init)
        [handler finalization-fn] (server-init-fn)
        port (parse-port port)]

    (log/info "starting " server-init "on" port)
    (log/info "java vendor: " (System/getProperty "java.vendor"))
    (log/info "java version: " (System/getProperty "java.version"))
    (log/info "java classpath: " (System/getProperty "java.class.path"))

    (let [server (start-container handler port)
          shutdown-fn (create-shutdown-fn server finalization-fn)]

      (log/info "started" server-init "on" port)
      shutdown-fn)))

(defn- server-cfg
  "Reads the server configuration from the environment. The variable
   SLIPSTREAM_WEBAPP_INIT must be defined. It is the namespaced symbol of the
   server initialization function.

   If the environmental variable SLIPSTREAM_WEBAPP_PORT is defined, the value
   will be used for the server port, presuming that it is valid. If the value
   is invalid, then the default port will be used.

   NOTE: This function is only called when starting the server from the main
   function. Starting the server from the REPL will use the values given to the
   start function."
  []
  (let [env (dyn-resolve 'environ.core/env)
        server-port (env :slipstream-webapp-port)]
    (if-let [server-init (env :slipstream-webapp-init)]
      [server-init server-port]
      (let [msg "SLIPSTREAM_WEBAPP_INIT is not defined"]
        (log/error msg)
        (throw (ex-info msg {}))))))

(defn -main
  "Function to start the web application as a daemon. The configuation of the
   server is taken from the environment. The environment must have
   SLIPSTREAM_WEBAPP_INIT and optionally SLIPSTREAM_WEBAPP_PORT defined."
  [& _]
  (let [[server-init server-port] (server-cfg)
        shutdown-fn (start server-init server-port)]
    (register-shutdown-hook shutdown-fn))

  ;; The server (started as a daemon thread) will exit immediately
  ;; if the main thread is allowed to terminate.  To avoid this,
  ;; indefinitely block this thread.  Stopping the service can only
  ;; be done externally through a SIGTERM signal.
  @(promise))
