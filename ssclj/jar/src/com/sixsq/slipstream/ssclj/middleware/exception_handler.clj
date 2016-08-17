(ns com.sixsq.slipstream.ssclj.middleware.exception-handler
  (:require
    [clojure.tools.logging :as log]
    [clj-stacktrace.repl :as st]
    [ring.util.response :as r]))

(defn treat-unexpected-exception
  [e]
  (let [msg      (str "Unexpected exception thrown: " (.getMessage e))
        body     {:status 500 :message msg}
        response (r/status (r/response body) 500)]
    (log/error msg "\n" (st/pst-str e))
    response))

(defn wrap-exceptions [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (let [response (ex-data e)]
             (if (r/response? response)
               response
               (treat-unexpected-exception e)))))))
