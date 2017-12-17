(ns com.sixsq.slipstream.ssclj.resources.callback-example
  (:require
    [com.sixsq.slipstream.ssclj.resources.callback :as callback]
    [com.sixsq.slipstream.ssclj.resources.callback.utils :as utils]
    [com.sixsq.slipstream.ssclj.util.log :as log-util]
    [clojure.tools.logging :as log]
    [com.sixsq.slipstream.util.response :as r]))

(def ^:const action-name "example")

(defmethod callback/execute action-name
  [{{:keys [ok?]} :data id :id :as resource}]
  (if ok?
    (do
      (utils/callback-succeeded! id)
      (log/info (format "executing action %s of %s succeeded" action-name id))
      (r/map-response "success" 200 id))
    (do
      (utils/callback-failed! id)
      (log-util/log-and-throw 400 (format "executing action %s of %s FAILED" action-name id)))))
