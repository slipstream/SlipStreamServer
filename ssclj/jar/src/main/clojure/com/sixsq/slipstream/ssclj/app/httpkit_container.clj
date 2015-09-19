(ns com.sixsq.slipstream.ssclj.app.httpkit-container
  (:require
    [clojure.tools.logging :as log]
    [org.httpkit.server :refer [run-server]]
    [com.sixsq.slipstream.ssclj.resources.common.debug-utils :as du]))

(defn start-container
  "Starts the http-kit container with the given ring handler and
   on the given port.  Returns the function to be called to shutdown
   the container."
  [handler port]
  (log/info "starting the application container on port" port)
  (run-server handler {:port port :ip "127.0.0.1"}))

