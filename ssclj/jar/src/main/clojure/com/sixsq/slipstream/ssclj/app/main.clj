(ns com.sixsq.slipstream.ssclj.app.main
  (:gen-class)
  (:require
    [com.sixsq.slipstream.ssclj.app.server :refer [start stop]]))

(def ^:const default-port 8200)

(defn- create-shutdown-hook
  [stop-fn]
  (proxy [Thread] [] (run [] (stop stop-fn))))

(defn register-shutdown-hook
  "Registers a shutdown hook in the JVM to shutdown the application
   container cleanly."
  [stop-fn]
  (let [hook (create-shutdown-hook stop-fn)]
    (.. (Runtime/getRuntime)
        (addShutdownHook hook))))

(defn parse-port
  "Parses the given string into an port value (int).  If the input
   string is not a valid number or not a valid port, nil is returned."
  [^String s]
  (try
    (let [port (Integer/valueOf s)]
      (if (< 0 port 65536) port))
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
