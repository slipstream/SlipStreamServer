(ns sixsq.slipstream.pricing.service.main
  (:gen-class))

(def ^:const default-port 15000)

(def ^:const server-ns 'sixsq.slipstream.pricing.service.server)

(def ^:const environ-ns 'environ.core)

(defn- hook
  "A JVM shutdown hook is just a Thread that runs a function
   in the 'run' method."
  [stop-fn]
  (proxy [Thread] [] (run [] (try
                               (and stop-fn (stop-fn))
                               (catch Exception e (println (str e)))))))

(defn register-shutdown-hook
  "Registers a shutdown hook in the JVM to shutdown the application
   container cleanly."
  [stop-fn]
  (.. (Runtime/getRuntime)
      (addShutdownHook (hook stop-fn))))

(defn parse-port
  "Parses the given string into an port value (int).  If the input
   string is not a valid number or not a valid port, nil is returned."
  [^String s]
  (try
    (let [port (Integer/valueOf s)]
      (if (< 0 port 65536) port))
    (catch Exception _
      nil)))

(defn env-port
  "Gets the port from the environment if possible."
  []
  (try
    (if-let [env (-> environ-ns
                     find-ns
                     (ns-resolve 'env))]
      (env :ss-port))
    (catch Exception _
      nil)))

(defn -main
  "Starts the application container on the port defined by the
   environment variable SS_PORT or the default port."
  [& _]
  (require server-ns)
  (let [start-fn (-> server-ns
                     find-ns
                     (ns-resolve 'start))]
    (-> (env-port)
        (parse-port)
        (or default-port)
        (start-fn)
        (register-shutdown-hook)))

  ;; The server (started as a daemon thread) will exit immediately
  ;; if the main thread is allowed to terminate.  To avoid this,
  ;; indefinitely block this thread.  Stopping the service can only
  ;; be done externally through a SIGTERM signal.
  @(promise))
