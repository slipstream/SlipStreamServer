(ns com.sixsq.slipstream.ssclj.util.log
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]))

(defn log-and-throw
  "Logs the given message and returns an error response with the
   given status code and message."
  [status msg]
  (log/error msg)
  (throw (ex-info msg {:status status, :message msg})))
