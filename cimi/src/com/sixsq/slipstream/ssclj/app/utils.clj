(ns com.sixsq.slipstream.ssclj.app.utils
  (:require
    [environ.core :as env]
    [com.sixsq.slipstream.ssclj.util.namespace-utils :as dyn]
    [sixsq.slipstream.server.namespace-utils :as nu]
    [clojure.tools.logging :as log]))

(defn call-fn-from-namespace
  [fn-name binding-ns]
  (if-let [binding-init-fn (nu/dyn-resolve (str binding-ns "/" fn-name))]
    (try
      (binding-init-fn)
      (catch Exception e
        (log/error "Exception occurred during db binding" binding-ns fn-name (str e))))
    (let [msg (str binding-ns "/" fn-name " could not be resolved")]
      (log/error msg)
      (throw (ex-info msg {})))))
