(ns com.sixsq.slipstream.ssclj.app.main
  (:gen-class)
  (:require
    [com.sixsq.slipstream.ssclj.app.server :refer [start register-shutdown-hook]]
    [clojure.tools.logging :as log]))

(defn valid-port?
  "If the port number is valid, then returns the port itself;
   otherwise returns nil."
  [port]
  (if (< 0 port 65536)
    port))

(defn parse-port
  "Parses the given string into a port value.  If the port is not
   valid, then function returns nil."
  [^String s]
  (try
    (valid-port? (Integer/valueOf s))
    (catch Exception e
      nil)))

(defn -main
  "Starts the cimi server using the command line arguments.  Takes as
   possible arguments the port number and Couchbase configuration file."
  [& [port]]
  (let [port (or (parse-port port) 8200)]
    (->> (start port)
         (register-shutdown-hook))))
