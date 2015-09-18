(ns com.sixsq.slipstream.ssclj.app.main
  (:gen-class)
  (:require
    [com.sixsq.slipstream.ssclj.app.server :refer [start stop]]))

(def ^:const default-port 8200)

(defn valid-port?
  "If the port number is valid, then returns the port itself;
   otherwise returns false."
  [port]
  (or (< 0 port 65536) port))

(defn- create-shutdown-hook
  [state]
  (proxy [Thread] [] (run [] (stop state))))

(defn register-shutdown-hook
  "Registers a shutdown hook in the JVM to shutdown the application
   container cleanly."
  [state]
  (let [hook (create-shutdown-hook state)]
    (.. (Runtime/getRuntime)
        (addShutdownHook hook))))

(defn parse-port
  "Parses the given string into a port value.  If the port is not
   valid, then function returns nil."
  [^String s]
  (try
    (valid-port? (Integer/valueOf s))
    (catch Exception _
      nil)))

(defn -main
  "Starts the application container using the given port (as a string)
   or the default port."
  [& [port]]
  (-> (parse-port port)
      (or default-port)
      (start)
      (register-shutdown-hook)))
