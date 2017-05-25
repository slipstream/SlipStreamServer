(ns com.sixsq.slipstream.ssclj.util.log
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [com.sixsq.slipstream.ssclj.util.response :as r]))

(defn log-and-throw
  "Logs the given message and returns an error response with the given status
   code and message."
  [status msg]
  (log/error status "-" msg)
  (throw (r/ex-response msg status)))
