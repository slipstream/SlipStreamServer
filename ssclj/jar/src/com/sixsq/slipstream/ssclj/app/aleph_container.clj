(ns com.sixsq.slipstream.ssclj.app.aleph-container
  (:require
    [clojure.tools.logging :as log]
    [aleph.http :as http]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du])
  (:import
    [java.io Closeable]
    [java.net InetSocketAddress]))

(defn create-stop-fn
  [^Closeable server]
  (fn [] (.close server)))

(defn start-container
  "Starts the aleph container with the given ring handler and
   on the given port.  Returns the function to be called to shutdown
   the container."
  [handler ^long port]
  (log/info "starting aleph application container on port" port)
  (->> port
       (InetSocketAddress. "127.0.0.1")
       (hash-map :socket-address)
       (http/start-server handler)
       (create-stop-fn)))

